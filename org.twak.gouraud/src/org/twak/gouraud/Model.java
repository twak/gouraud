package org.twak.gouraud;

import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_AMBIENT;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_SPECULAR;
import static org.lwjgl.assimp.Assimp.aiGetErrorString;
import static org.lwjgl.assimp.Assimp.aiGetMaterialColor;
import static org.lwjgl.assimp.Assimp.aiReleaseImport;
import static org.lwjgl.assimp.Assimp.aiTextureType_NONE;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_STATIC_DRAW_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.glBindBufferARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.glBufferDataARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.glGenBuffersARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.nglBufferDataARB;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryUtil;

class Model {

    public AIScene scene;
    public List<Model.Mesh> meshes;
    public List<Model.Material> materials;

    public Model(AIScene scene) {

        this.scene = scene;

        int meshCount = scene.mNumMeshes();
        PointerBuffer meshesBuffer = scene.mMeshes();
        meshes = new ArrayList<>();
        for (int i = 0; i < meshCount; ++i) {
            meshes.add(new Mesh(AIMesh.create(meshesBuffer.get(i))));
        }

        int materialCount = scene.mNumMaterials();
        PointerBuffer materialsBuffer = scene.mMaterials();
        materials = new ArrayList<>();
        for (int i = 0; i < materialCount; ++i) {
            materials.add(new Material(AIMaterial.create(materialsBuffer.get(i))));
        }
    }

    public void free() {
        aiReleaseImport(scene);
        scene = null;
        meshes = null;
        materials = null;
    }

    public static class Mesh {

        public AIMesh mesh;
        public int vertexArrayBuffer;
        public int normalArrayBuffer;
        public int colourArrayBuffer;
        public int elementArrayBuffer;
        public int elementCount;

        public Mesh(AIMesh mesh) {
            this.mesh = mesh;

            vertexArrayBuffer = glGenBuffersARB();
            glBindBufferARB(GL_ARRAY_BUFFER_ARB, vertexArrayBuffer);
            AIVector3D.Buffer vertices = mesh.mVertices();
            nglBufferDataARB(GL_ARRAY_BUFFER_ARB, AIVector3D.SIZEOF * vertices.remaining(),
                    vertices.address(), GL_STATIC_DRAW_ARB);

            normalArrayBuffer = glGenBuffersARB();
            glBindBufferARB(GL_ARRAY_BUFFER_ARB, normalArrayBuffer);
            AIVector3D.Buffer normals = mesh.mNormals();
            nglBufferDataARB(GL_ARRAY_BUFFER_ARB, AIVector3D.SIZEOF * normals.remaining(),
                    normals.address(), GL_STATIC_DRAW_ARB);
            
            colourArrayBuffer = glGenBuffersARB();
            glBindBufferARB(GL_ARRAY_BUFFER_ARB, colourArrayBuffer);
            
            float[] colors = new float[vertices.remaining() * 3 * 4];
            
            for (int i = 0; i < colors.length; i++)
            		colors[i] = (float)Math.random();
            
            FloatBuffer colorsBuffer = BufferUtils.createFloatBuffer(colors.length);
            colorsBuffer.put(colors);
            colorsBuffer.flip();
            
            glBufferDataARB(GL_ARRAY_BUFFER_ARB, colorsBuffer, GL_STATIC_DRAW_ARB);
//            nglBufferDataARB(GL_ARRAY_BUFFER_ARB, AIVector3D.SIZEOF * 24,  MemoryUtil.getAddress(fb), GL_STATIC_DRAW_ARB);
//            PointerBuffer colours =  PointerBuffer.allocateDirect(capacity) mesh.mColors();//mNormals();
            
            int faceCount = mesh.mNumFaces();
            elementCount = faceCount * 3;
            IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
            AIFace.Buffer facesBuffer = mesh.mFaces();
            
            for (int i = 0; i < faceCount; ++i) {
                AIFace face = facesBuffer.get(i);
                if (face.mNumIndices() != 3) {
                    throw new IllegalStateException("AIFace.mNumIndices() != 3");
                }
                elementArrayBufferData.put(face.mIndices());
            }
            
            elementArrayBufferData.flip();
            elementArrayBuffer = glGenBuffersARB();
            glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, elementArrayBuffer);
            glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, elementArrayBufferData, GL_STATIC_DRAW_ARB);
        }
    }

    public static class Material {

        public AIMaterial mMaterial;
        public AIColor4D mAmbientColor;
        public AIColor4D mDiffuseColor;
        public AIColor4D mSpecularColor;

        public Material(AIMaterial material) {

            mMaterial = material;

            mAmbientColor = AIColor4D.create();
            if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_AMBIENT,
                    aiTextureType_NONE, 0, mAmbientColor) != 0) {
                throw new IllegalStateException(aiGetErrorString());
            }
            mDiffuseColor = AIColor4D.create();
            if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_DIFFUSE,
                    aiTextureType_NONE, 0, mDiffuseColor) != 0) {
                throw new IllegalStateException(aiGetErrorString());
            }
            mSpecularColor = AIColor4D.create();
            if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_SPECULAR,
                    aiTextureType_NONE, 0, mSpecularColor) != 0) {
                throw new IllegalStateException(aiGetErrorString());
            }
        }
    }
}