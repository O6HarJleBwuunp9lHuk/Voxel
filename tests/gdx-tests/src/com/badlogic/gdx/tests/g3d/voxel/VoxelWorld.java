// VoxelWorld.java
package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btVector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.utils.Disposable;

public class VoxelWorld implements RenderableProvider {
	public static final int CHUNK_SIZE_X = 16;
	public static final int CHUNK_SIZE_Y = 16;
	public static final int CHUNK_SIZE_Z = 16;

    public static final float WORLD_SCALE = 1f;

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

    public static final int PROJECTILE_MARKER = 1;  // Маркер для снарядов
    public static final int VOXEL_MARKER = 2;      // Маркер для вокселей
    public static final int GROUND_MARKER = 3;     // Маркер для земли

    // Группы коллизий (должны быть степенями 2)
    public static final short GROUND_GROUP = 1 << 0;  // 1
    public static final short VOXEL_GROUP = 1 << 1;   // 2
    public static final short PROJECTILE_GROUP = 1 << 2; // 4


    public static final short PROJECTILE_MASK = VOXEL_GROUP | GROUND_GROUP;
    public static final short VOXEL_MASK = PROJECTILE_GROUP;

	public int renderedChunks;
	public int numChunks;



	public final int[] palette = new int[256];
	public boolean useColors = false;
	final int[][][] voxelColors;

    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btBroadphaseInterface broadphase;
    private btConstraintSolver solver;
    public btDynamicsWorld dynamicsWorld;
    float mass;



    Array<btRigidBody> physicsBodies = new Array<>();

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
        float mass = 0f;



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

        Bullet.init();
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -9.8f, 0));
        //dynamicsWorld.setContactListener(new VoxelContactListener(this));
        //dynamicsWorld.addRigidBody(groundBody, STATIC_GROUP, PROJECTILE_MASK);
        btBoxShape groundShape = new btBoxShape(new Vector3(100, 1, 100));
        btRigidBody.btRigidBodyConstructionInfo groundInfo =
            new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape);

        btRigidBody ground = new btRigidBody(groundInfo);
        ground.setWorldTransform(new Matrix4().setToTranslation(0, 0, 0));
        ground.setUserValue(VoxelWorld.GROUND_MARKER);
        ground.setUserPointer(System.nanoTime());// Маркер для земли

        // Добавляем в мир
        this.addBody(ground, GROUND_GROUP, PROJECTILE_MASK);


	}

    public void update(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime);
        checkProjectileCollisions();

       // VoxelContactListener callback = new VoxelContactListener(this);
        //dynamicsWorld.contactTest(callback);
        //callback.dispose();
    }

    public void checkProjectileCollisions() {
        // Получаем все объекты в мире
        btCollisionObjectArray objects = dynamicsWorld.getCollisionObjectArray();

        // Собираем снаряды и воксели в отдельные списки
        Array<btRigidBody> projectiles = new Array<>();
        Array<btRigidBody> voxels = new Array<>();

        for (int i = 0; i < objects.size(); i++) {
            btCollisionObject obj = objects.atConst(i);
            int userValue = obj.getUserValue();
            //System.out.println("Object " + i + " has userValue: " + userValue);
            if (obj.getUserValue() == PROJECTILE_MARKER) {
                projectiles.add((btRigidBody)obj);
            } else if (obj.getUserValue() == VOXEL_MARKER) {
                voxels.add((btRigidBody)obj);
            }
        }
        System.out.println(projectiles.size);
        System.out.println(voxels.size);

        // Проверяем столкновения между снарядами и вокселями
        for (btRigidBody projectile : projectiles) {
            for (btRigidBody voxel : voxels) {
                if (checkObjectsCollision(projectile, voxel)) {
                    handleCollision(projectile, voxel);
                    break; // Обрабатываем только одно столкновение за кадр
                }
            }
        }
    }

    private boolean checkObjectsCollision(btRigidBody obj1, btRigidBody obj2) {
        // Получаем позиции объектов
        Vector3 pos1 = new Vector3();
        Vector3 pos2 = new Vector3();
        Matrix4 transform = new Matrix4();
        obj1.getWorldTransform(transform);
        pos1 = transform.getTranslation(new Vector3());
        obj2.getWorldTransform(transform);
        pos2 = transform.getTranslation(new Vector3());


        // Простая проверка расстояния (можно улучшить)
        float distance = pos1.dst(pos2);
        float collisionDistance = WORLD_SCALE * 1.5f; // Эмпирическое значение

        return distance < collisionDistance;
    }

    private void handleCollision(btRigidBody projectile, btRigidBody voxel) {
        // Получаем данные вокселя
        VoxelData data = decodeVoxelData(voxel.getUserPointer());

        // Получаем скорость снаряда
        Vector3 projectileVel = new Vector3();
        projectileVel = projectile.getLinearVelocity();

        // Вычисляем направление и силу удара
        Vector3 hitDirection = projectileVel.cpy().nor();
        float forceMagnitude = projectileVel.len() * 5f; // Усиливаем эффект

        // Применяем импульс к вокселю
        Vector3 impulse = hitDirection.scl(forceMagnitude);
        voxel.applyCentralImpulse(impulse);

        // Делаем воксель динамическим (если он был статичным)
        voxel.setMassProps(1f, new Vector3()); // Масса 1
        voxel.setActivationState(Collision.ACTIVE_TAG);

        // Удаляем снаряд
        //removeBody(projectile);

        System.out.printf("Voxel at [%d,%d,%d] hit with force %.2f\n",
            data.x, data.y, data.z, forceMagnitude);
    }

    public void addPhysicsBody(btRigidBody body) {
        dynamicsWorld.addRigidBody(body);
        System.out.println("Bodies in world: " + dynamicsWorld.getNumCollisionObjects());
        physicsBodies.add(body);

    }

    public void dispose() {
        for (btRigidBody body : physicsBodies) {
            dynamicsWorld.removeRigidBody(body);
            body.dispose();
        }
        physicsBodies.clear();

        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
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
        if (x < 0 || x >= voxelsX || y < 0 || y >= voxelsY || z < 0 || z >= voxelsZ) return;

        removeVoxelBody(x, y, z); // Удаляем старое тело если было

        if (voxelType != 0) {
            btBoxShape shape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f).scl(WORLD_SCALE));

            btMotionState motionState = new btDefaultMotionState();
            btRigidBody.btRigidBodyConstructionInfo info =
                new btRigidBody.btRigidBodyConstructionInfo(1f, motionState, shape);

            btRigidBody body = new btRigidBody(info);
            Vector3 pos = new Vector3(x + 0.5f, y + 0.5f, z + 0.5f).scl(WORLD_SCALE);
            body.setWorldTransform(new Matrix4().setToTranslation(pos));

            // Сохраняем цвет в пользовательских данных
            body.setUserValue(VOXEL_MARKER);
            VoxelData data = new VoxelData(color, x, y, z);
            body.setUserPointer(encodeVoxelData(data));
            // Дополнительно сохраняем изначальные координаты (если нужно)
            //body.setUserPointer(hashPosition(x, y, z));

            body.setRestitution(0.3f);
            body.setFriction(0.7f);
            body.setActivationState(Collision.DISABLE_DEACTIVATION);

            addBody(body, VOXEL_GROUP, (PROJECTILE_MASK));
        }
        // Обновляем чанк (если используете чанковую систему)
        updateChunkAt(x, y, z);
    }


    private void removeVoxelBody(int x, int y, int z) {
        long key = hashPosition(x, y, z);
        for (int i = 0; i < physicsBodies.size; i++) {
            if (physicsBodies.get(i).getUserPointer() == key) {
                btRigidBody body = physicsBodies.removeIndex(i);
                dynamicsWorld.removeRigidBody(body);
                body.dispose();
                break;
            }
        }
    }

    private long encodeVoxelData(VoxelData data) {
        return ((long)data.color << 32) |
            ((long)data.x << 16) |
            ((long)data.y << 8) |
            data.z;
    }

    // Декодируем данные из userPointer
    VoxelData decodeVoxelData(long userPointer) {
        int color = (int)(userPointer >>> 32);
        int x = (int)((userPointer >> 16) & 0xFF);
        int y = (int)((userPointer >> 8) & 0xFF);
        int z = (int)(userPointer & 0xFF);
        return new VoxelData(color, x, y, z);
    }

    private void updateChunkAt(int x, int y, int z) {
        int chunkX = x / CHUNK_SIZE_X;
        int chunkY = y / CHUNK_SIZE_Y;
        int chunkZ = z / CHUNK_SIZE_Z;
        int chunkIndex = chunkX + chunkZ * chunksX + chunkY * chunksX * chunksZ;
        dirty[chunkIndex] = true;
    }

    private long hashPosition(int x, int y, int z) {
        // Используем битовые сдвиги для создания уникального хеша из координат
        return ((long)x & 0xFFFF) << 32 | ((long)y & 0xFFFF) << 16 | ((long)z & 0xFFFF);
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

    public void addBody(btRigidBody body, short group, short mask) {
        dynamicsWorld.addRigidBody(body, group, mask);
        physicsBodies.add(body);
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

    public void breakVoxel(int x, int y, int z, Vector3 impactPoint, float force) {
        if (get(x, y, z) == 0) return;

        System.out.println("Destroying voxel at: " + x + ", " + y + ", " + z);
        setVoxelWithColor(x, y, z, (byte)0, 0); // Удаляем воксель

        // Опционально: создаем эффект разрушения (частицы, звук)
        // ...
    }

    public void setMass(float mass) {
        mass = 1f;
    }

    public float getMass() {
        return this.mass;
    }

}
