// VoxelWorld.java
package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class VoxelWorld implements RenderableProvider {
	public static final int CHUNK_SIZE_X = 16;
	public static final int CHUNK_SIZE_Y = 16;
	public static final int CHUNK_SIZE_Z = 16;

	public final VoxelChunk[] chunks;
	public final Mesh[] meshes;
	public final Material[] materials;
	public final boolean[] dirty;
	public final int[] numVertices;
	public float[] vertices;

	public final int chunksX;
	public final int chunksY;
	public final int chunksZ;
	public final int voxelsX;
	public final int voxelsY;
	public final int voxelsZ;

	public int renderedChunks;
	public int numChunks;

	public final int[] palette = new int[256];
	public boolean useColors = false;
	private final int[][][] voxelColors;

	public VoxelWorld( int chunksX, int chunksY, int chunksZ) {
		this.chunksX = chunksX;
		this.chunksY = chunksY;
		this.chunksZ = chunksZ;
		this.numChunks = chunksX * chunksY * chunksZ;
		this.voxelsX = chunksX * CHUNK_SIZE_X;
		this.voxelsY = chunksY * CHUNK_SIZE_Y;
		this.voxelsZ = chunksZ * CHUNK_SIZE_Z;
		this.chunks = new VoxelChunk[numChunks];
		this.meshes = new Mesh[numChunks];
		this.materials = new Material[numChunks];
		this.dirty = new boolean[numChunks];
		this.numVertices = new int[numChunks];
		this.voxelColors = new int[voxelsX][voxelsY][voxelsZ];

		int index = 0;
		for (int y = 0; y < chunksY; y++) {
			for (int z = 0; z < chunksZ; z++) {
				for (int x = 0; x < chunksX; x++) {
					VoxelChunk chunk = new VoxelChunk(CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z);
					chunk.offset.set(x * CHUNK_SIZE_X, y * CHUNK_SIZE_Y, z * CHUNK_SIZE_Z);
					chunks[index] = chunk;
					dirty[index] = true;
					numVertices[index] = 0;
					index++;
				}
			}
		}

		createMeshesAndMaterials();
	}

	private void createMeshesAndMaterials() {
		VertexAttribute[] attributes = new VertexAttribute[] {
				VertexAttribute.Position(),
				VertexAttribute.Normal(),
				VertexAttribute.ColorUnpacked()
		};

		int indicesCount = CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z * 6 * 6 / 3;
		short[] indices = new short[indicesCount];
		short vertexIndex = 0;

		for (int i = 0; i < indicesCount; i += 6, vertexIndex += 4) {
			indices[i] = vertexIndex;
			indices[i+1] = (short)(vertexIndex + 1);
			indices[i+2] = (short)(vertexIndex + 2);
			indices[i+3] = (short)(vertexIndex + 2);
			indices[i+4] = (short)(vertexIndex + 3);
			indices[i+5] = vertexIndex;
		}

		for (int i = 0; i < numChunks; i++) {
			meshes[i] = new Mesh(true,
					CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z * 6 * 4,
					CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z * 36 / 3,
					attributes);

			meshes[i].setIndices(indices);

			materials[i] = new Material();
		}

		this.vertices = new float[(7) * 6 * CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z];
	}

	public void setVoxelWithColor(int x, int y, int z, byte voxelType, int color) {
		if (x < 0 || x >= voxelsX || y < 0 || y >= voxelsY || z < 0 || z >= voxelsZ) {
			return;
		}
		voxelColors[x][y][z] = color;

		int chunkX = x / CHUNK_SIZE_X;
		int chunkY = y / CHUNK_SIZE_Y;
		int chunkZ = z / CHUNK_SIZE_Z;
		int chunkIndex = chunkX + chunkZ * chunksX + chunkY * chunksX * chunksZ;

		chunks[chunkIndex].set(
				x % CHUNK_SIZE_X,
				y % CHUNK_SIZE_Y,
				z % CHUNK_SIZE_Z,
				voxelType
		);

		dirty[chunkIndex] = true;
	}

	public byte get(float x, float y, float z) {
		int ix = (int)x;
		int iy = (int)y;
		int iz = (int)z;

		if (ix < 0 || ix >= voxelsX || iy < 0 || iy >= voxelsY || iz < 0 || iz >= voxelsZ) {
			return 0;
		}

		int chunkX = ix / CHUNK_SIZE_X;
		int chunkY = iy / CHUNK_SIZE_Y;
		int chunkZ = iz / CHUNK_SIZE_Z;

		return chunks[chunkX + chunkZ * chunksX + chunkY * chunksX * chunksZ]
				.get(ix % CHUNK_SIZE_X, iy % CHUNK_SIZE_Y, iz % CHUNK_SIZE_Z);
	}

	public float getHighest(float x, float z) {
		int ix = (int)x;
		int iz = (int)z;

		if (ix < 0 || ix >= voxelsX || iz < 0 || iz >= voxelsZ) {
			return 0;
		}

		for (int y = voxelsY - 1; y >= 0; y--) {
			if (get(ix, y, iz) > 0) {
				return y + 1;
			}
		}
		return 0;
	}

	@Override
	public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
		renderedChunks = 0;

		for (int i = 0; i < numChunks; i++) {
			VoxelChunk chunk = chunks[i];
			Mesh mesh = meshes[i];

			if (dirty[i]) {
				int numVerts = chunk.calculateVertices(vertices, voxelColors);
				numVertices[i] = numVerts / 4 * 6;
				mesh.setVertices(vertices, 0, numVerts * 7);

				dirty[i] = false;
			}

			if (numVertices[i] > 0) {
				Renderable renderable = pool.obtain();
				renderable.material = materials[i];
				renderable.meshPart.mesh = mesh;
				renderable.meshPart.offset = 0;
				renderable.meshPart.size = numVertices[i];
				renderable.meshPart.primitiveType = GL20.GL_TRIANGLES;
				renderables.add(renderable);
				renderedChunks++;
			}
		}
	}
}