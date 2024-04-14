package com.platuro.delivery.Until;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ChunkHandler {
    // create a List of Chunks to keep track of the loaded chunks
    private List<Chunk> loadedChunks = new ArrayList<>();

    public void loadAdjacentChunks(World world, int chunkX, int chunkZ) {
        // Central chunk
        forceLoadChunk(world, chunkX, chunkZ);

        // Load surrounding chunks
        forceLoadChunk(world, chunkX + 1, chunkZ);
        forceLoadChunk(world, chunkX - 1, chunkZ);
        forceLoadChunk(world, chunkX, chunkZ + 1);
        forceLoadChunk(world, chunkX, chunkZ - 1);

        // Optionally, load diagonal chunks
        forceLoadChunk(world, chunkX + 1, chunkZ + 1);
        forceLoadChunk(world, chunkX - 1, chunkZ - 1);
        forceLoadChunk(world, chunkX + 1, chunkZ - 1);
        forceLoadChunk(world, chunkX - 1, chunkZ + 1);
    }

    private void forceLoadChunk(World world, int x, int z) {
        Chunk chunk = world.getChunkAt(x, z);
        chunk.load(true); // true to keep the chunk loaded
        chunk.setForceLoaded(true);
        loadedChunks.add(chunk);
    }

    public void unloadChunks() {
        for (Chunk chunk : loadedChunks) {
            chunk.setForceLoaded(false);
            chunk.unload(true);
        }
        loadedChunks.clear();
    }
}
