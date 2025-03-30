package org.printed.chat;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TextureLoader {

    private static Map<String, Integer> textureCache = new HashMap<>();

    public static int loadTexture(String fileName) {
        // Verificar si la textura ya está cargada
        if (textureCache.containsKey(fileName)) {
            return textureCache.get(fileName);
        }

        // Generar ID de textura
        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Configurar parámetros de la textura
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Cargar imagen
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Obtener la ruta del archivo de imagen
        Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "textures", fileName);
        String filePath = path.toString();

        // Voltear la imagen verticalmente para que coincida con las coordenadas de OpenGL
        STBImage.stbi_set_flip_vertically_on_load(false);

        // Cargar la imagen
        ByteBuffer image = STBImage.stbi_load(filePath, width, height, channels, 4);

        if (image == null) {
            System.err.println("Error al cargar la textura: " + STBImage.stbi_failure_reason());
            return -1;
        }

        // Cargar datos de la imagen a la textura
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(0), height.get(0),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // Liberar la memoria
        STBImage.stbi_image_free(image);

        // Desenlazar la textura
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Guardar en caché
        textureCache.put(fileName, textureId);

        return textureId;
    }

    public static void cleanup() {
        textureCache.values().forEach(GL11::glDeleteTextures);
        textureCache.clear();
    }
}