package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.math.Vector3;

public class VoxelChunk {
	public static final int VERTEX_SIZE = 6;
	public final byte[] voxels;
	public final int width;
	public final int height;
	public final int depth;
	public final Vector3 offset = new Vector3();
	private final int widthTimesHeight;
	private final int topOffset;
	private final int bottomOffset;
	private final int leftOffset;
	private final int rightOffset;
	private final int frontOffset;
	private final int backOffset;

	public VoxelChunk (int width, int height, int depth) {
		this.voxels = new byte[width * height * depth];
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.topOffset = width * depth;
		this.bottomOffset = -width * depth;
		this.leftOffset = -1;
		this.rightOffset = 1;
		this.frontOffset = -width;
		this.backOffset = width;
		this.widthTimesHeight = width * height;
	}

	public byte get (int x, int y, int z) {
		if (x < 0 || x >= width) return 0;
		if (y < 0 || y >= height) return 0;
		if (z < 0 || z >= depth) return 0;
		return getFast(x, y, z);
	}

	public byte getFast (int x, int y, int z) {
		return voxels[x + z * width + y * widthTimesHeight];
	}

	public void set (int x, int y, int z, byte voxel) {
		if (x < 0 || x >= width) return;
		if (y < 0 || y >= height) return;
		if (z < 0 || z >= depth) return;
		setFast(x, y, z, voxel);
	}

	public void setFast (int x, int y, int z, byte voxel) {
		voxels[x + z * width + y * widthTimesHeight] = voxel;
	}

	public int calculateVertices (float[] vertices, int[][][] voxelColors) {
		int i = 0;
		int vertexOffset = 0;
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < depth; z++) {
				for (int x = 0; x < width; x++, i++) {
					byte voxel = voxels[i];
					if (voxel == 0) continue;

					int globalX = (int)offset.x + x;
					int globalY = (int)offset.y + y;
					int globalZ = (int)offset.z + z;
					int color = voxelColors[globalX][globalY][globalZ];


					if (y < height - 1) {
						if (voxels[i + topOffset] == 0) vertexOffset = createTop(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createTop(offset, x, y, z, vertices, vertexOffset, color);
					}
					if (y > 0) {
						if (voxels[i + bottomOffset] == 0) vertexOffset = createBottom(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createBottom(offset, x, y, z, vertices, vertexOffset, color);
					}
					if (x > 0) {
						if (voxels[i + leftOffset] == 0) vertexOffset = createLeft(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createLeft(offset, x, y, z, vertices, vertexOffset, color);
					}
					if (x < width - 1) {
						if (voxels[i + rightOffset] == 0) vertexOffset = createRight(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createRight(offset, x, y, z, vertices, vertexOffset, color);
					}
					if (z > 0) {
						if (voxels[i + frontOffset] == 0) vertexOffset = createFront(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createFront(offset, x, y, z, vertices, vertexOffset, color);
					}
					if (z < depth - 1) {
						if (voxels[i + backOffset] == 0) vertexOffset = createBack(offset, x, y, z, vertices, vertexOffset, color);
					} else {
						vertexOffset = createBack(offset, x, y, z, vertices, vertexOffset, color);
					}
				}
			}
		}
		return vertexOffset ;
	}

	public static int createTop (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;

		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;

		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;


		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}

	public static int createBottom (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}

	public static int createLeft (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}

	public static int createRight (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}

	public static int createFront (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}

	public static int createBack (Vector3 offset, int x, int y, int z, float[] vertices, int vertexOffset, int color) {
		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		float r = ((color >> 12) & 0xF) / 15f;
		float g = ((color >> 8) & 0xF) / 15f;
		float b = ((color >> 4)  & 0xF) / 15f;
		float a = (color & 0xF) / 15f;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y + 1;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;

		vertices[vertexOffset++] = offset.x + x + 1;
		vertices[vertexOffset++] = offset.y + y;
		vertices[vertexOffset++] = offset.z + z + 1;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = 0;
		vertices[vertexOffset++] = -1;
		vertices[vertexOffset++] = r;
		vertices[vertexOffset++] = g;
		vertices[vertexOffset++] = b;
		vertices[vertexOffset++] = a;
		return vertexOffset;
	}
}
