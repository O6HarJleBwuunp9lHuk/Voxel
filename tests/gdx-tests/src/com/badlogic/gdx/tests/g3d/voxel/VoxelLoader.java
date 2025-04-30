package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.graphics.Color;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VoxelLoader {

    public static VoxelWorld loadFromVoxFile(String filePath, int chunksX, int chunksY, int chunksZ) throws IOException {
        int sizeX = 0;
        int sizeY = 0;
        int offsetX = 50;
        int offsetY = 10;
        int offsetZ = 50;
        VoxelWorld world = new VoxelWorld( chunksX, chunksY, chunksZ);
        byte[] fileData = readFileToByteArray(filePath);
        ByteBuffer buffer = ByteBuffer.wrap(fileData).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[4];
        buffer.get(magic);
        if (!(magic[0] == 'V' && magic[1] == 'O' && magic[2] == 'X' && magic[3] == ' ')) {
            throw new IOException("Not a valid .vox file");
        }

        buffer.getInt();
        int chunkStart0= buffer.position();
        while (buffer.remaining() >= 12) {
            String chunkId = readChunkId(buffer);
            int chunkSize = buffer.getInt();
            int childChunks = buffer.getInt();
            int chunkStart = buffer.position();
         if (chunkId.equals("RGBA")) {
             buffer.position(buffer.position() + 4);
             for (int i = 1; i < 256; i++) {
                 int r4 = (buffer.get() & 0xFF) >> 4; // 0-15
                 int g4 = (buffer.get() & 0xFF) >> 4;
                 int b4 = (buffer.get() & 0xFF) >> 4;
                 int a4 = (buffer.get() & 0xFF) >> 4;
                 short color = (short)((r4 << 12) | (g4 << 8) | (b4 << 4) | a4);
                 world.palette[i] = color;
             }
        }
            buffer.position(chunkStart + chunkSize);
        }
        buffer.position(chunkStart0);
            while (buffer.remaining() >= 12) {
                String chunkId = readChunkId(buffer);
                int chunkSize = buffer.getInt();
                int childChunks = buffer.getInt();
                int chunkStart = buffer.position();

                if (chunkId.equals("SIZE")) {
                    sizeX = buffer.getInt();
                    sizeY = buffer.getInt();
                    int sizeZ = buffer.getInt();
                    offsetX = (world.voxelsX - sizeX) / 2; // Центр по X
                    offsetZ = (world.voxelsZ - sizeZ) / 2;
                } else if (chunkId.equals("XYZI")) {
                    int numVoxels = buffer.getInt();
                    for (int i = 0; i < numVoxels; i++) {
                        int x = (buffer.get() & 0xFF) ;
                        int y = ((buffer.get() & 0xFF) * - 1) + sizeY;
                        int z = (buffer.get() & 0xFF);
                        int colorIndex = buffer.get() & 0xFF;

                        if (colorIndex > 0) {
                            int color = world.palette[colorIndex];
                            world.setVoxelWithColor(y + offsetX, z, x + offsetZ, (byte) 1, color);
                        }
                    }
                }
                buffer.position(chunkStart + chunkSize);
            }

        return world;
    }

    private static String readChunkId(ByteBuffer buffer) {
        byte[] idBytes = new byte[4];
        buffer.get(idBytes);
        return new String(idBytes);
    }

    private static byte[] readFileToByteArray(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
}