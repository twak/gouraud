package org.twak.gouraud;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.assimp.Assimp.*;
import static org.twak.gouraud.DemoUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBFragmentShader.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.ARBVertexShader.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Shows how to load models in Wavefront obj and mlt format with Assimp binding
 * and render them with OpenGL.
 *
 * @author Zhang Hai
 */
public class ObjLoader {

	long window;
	int width = 1024;
	int height = 768;
	int fbWidth = 1024;
	int fbHeight = 768;
	float fov = 60;
	float rotation;

	int program;
	int vertexAttribute;
	int normalAttribute;
	int colourAttribute;
	int modelMatrixUniform;
	int viewMatrixUniform;
	int projectionMatrixUniform;
	int normalMatrixUniform;
	int lightPositionUniform;
	int viewPositionUniform;
	int ambientColorUniform;
	int diffuseColorUniform;
	int specularColorUniform;

	Model model;

	Matrix4f modelMatrix = new Matrix4f().rotateY(0.5f * (float) Math.PI).scale(1.5f, 1.5f, 1.5f);
	Matrix4f projectionMatrix = new Matrix4f();
	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projectMatrix = new Matrix4f();
	Vector3f viewPosition = new Vector3f();
	Vector3f lightPosition = new Vector3f(10f, 5f, 10f);

	private FloatBuffer modelMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
	private FloatBuffer viewMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
	private FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
	private Matrix3f normalMatrix = new Matrix3f();
	private FloatBuffer normalMatrixBuffer = BufferUtils.createFloatBuffer(3 * 3);
	private FloatBuffer lightPositionBuffer = BufferUtils.createFloatBuffer(3);
	private FloatBuffer viewPositionBuffer = BufferUtils.createFloatBuffer(3);

	GLCapabilities caps;
	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	GLFWWindowSizeCallback wsCallback;
	GLFWCursorPosCallback cpCallback;
	GLFWScrollCallback sCallback;
	Callback debugProc;

	String shader;

	public ObjLoader(String shader) {
		this.shader = shader;
	}

	void init() throws IOException {

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
		window = glfwCreateWindow(width, height, "Shading demo", NULL, NULL);
		if (window == NULL)
			throw new AssertionError("Failed to create the GLFW window");

		System.out.println("Move the mouse to look around");
		System.out.println("Zoom in/out with mouse wheel");
		glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (ObjLoader.this.fbWidth != width || ObjLoader.this.fbHeight != height)) {
					ObjLoader.this.fbWidth = width;
					ObjLoader.this.fbHeight = height;
				}
			}
		});
		glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (ObjLoader.this.width != width || ObjLoader.this.height != height)) {
					ObjLoader.this.width = width;
					ObjLoader.this.height = height;
				}
			}
		});
		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action != GLFW_RELEASE) {
					return;
				}
				if (key == GLFW_KEY_ESCAPE) {
					glfwSetWindowShouldClose(window, true);
				}
			}
		});
		glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double x, double y) {
				rotation = ((float) x / width - 0.5f) * 2f * (float) Math.PI;
			}
		});
		glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				if (yoffset < 0) {
					fov *= 1.05f;
				} else {
					fov *= 1f / 1.05f;
				}
				if (fov < 10.0f) {
					fov = 10.0f;
				} else if (fov > 120.0f) {
					fov = 120.0f;
				}
			}
		});

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		glfwMakeContextCurrent(window);
		glfwSwapInterval(0);
		glfwShowWindow(window);
		glfwSetCursorPos(window, width / 2, height / 2);

		IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
		nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
		fbWidth = framebufferSize.get(0);
		fbHeight = framebufferSize.get(1);

		caps = GL.createCapabilities();
		if (!caps.GL_ARB_shader_objects) {
			throw new AssertionError("This demo requires the ARB_shader_objects extension.");
		}
		if (!caps.GL_ARB_vertex_shader) {
			throw new AssertionError("This demo requires the ARB_vertex_shader extension.");
		}
		if (!caps.GL_ARB_fragment_shader) {
			throw new AssertionError("This demo requires the ARB_fragment_shader extension.");
		}
		debugProc = GLUtil.setupDebugMessageCallback();

		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_DEPTH_TEST);

		/* Create all needed GL resources */
		loadModel();
		createProgram();
	}

	void loadModel() {
		String fileName = "C:\\Users\\twak\\Desktop\\sphere.obj";
		File file = new File(fileName);
		AIScene scene = aiImportFile(file.getAbsolutePath(), aiProcess_JoinIdenticalVertices | aiProcess_Triangulate);
		
		if (scene == null)
			throw new IllegalStateException(aiGetErrorString());

		model = new Model(scene);
	}

	static int createShader(String resource, int type) throws IOException {
		int shader = glCreateShaderObjectARB(type);
		ByteBuffer source = ioResourceToByteBuffer(new File(".", resource).getAbsolutePath(), 1024);
		PointerBuffer strings = BufferUtils.createPointerBuffer(1);
		IntBuffer lengths = BufferUtils.createIntBuffer(1);
		strings.put(0, source);
		lengths.put(0, source.remaining());
		glShaderSourceARB(shader, strings, lengths);
		glCompileShaderARB(shader);
		int compiled = glGetObjectParameteriARB(shader, GL_OBJECT_COMPILE_STATUS_ARB);
		String shaderLog = glGetInfoLogARB(shader);
		if (shaderLog != null && shaderLog.trim().length() > 0) {
			System.err.println(shaderLog);
		}
		if (compiled == 0) {
			throw new AssertionError("Could not compile shader");
		}
		return shader;
	}

	void createProgram() throws IOException {

		program = glCreateProgramObjectARB();
		int vertexShader = createShader(shader + ".vs", GL_VERTEX_SHADER_ARB);
		int fragmentShader = createShader(shader + ".fs", GL_FRAGMENT_SHADER_ARB);
		glAttachObjectARB(program, vertexShader);
		glAttachObjectARB(program, fragmentShader);
		glLinkProgramARB(program);
		int linkStatus = glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB);
		String programLog = glGetInfoLogARB(program);
		if (programLog != null && programLog.trim().length() > 0) {
			System.err.println(programLog);
		}
		if (linkStatus == 0) {
			throw new AssertionError("Could not link program");
		}

		glUseProgramObjectARB(program);

		vertexAttribute = glGetAttribLocationARB(program, "aVertex");
		glEnableVertexAttribArrayARB(vertexAttribute);

		normalAttribute = glGetAttribLocationARB(program, "aNormal");
		if (normalAttribute != GL_NO_ERROR)
			glEnableVertexAttribArrayARB(normalAttribute);

		colourAttribute = glGetAttribLocationARB(program, "aColour");
		if (colourAttribute != GL_NO_ERROR)
			glEnableVertexAttribArrayARB(colourAttribute);

		modelMatrixUniform = glGetUniformLocationARB(program, "uModelMatrix");

		viewMatrixUniform = glGetUniformLocationARB(program, "uViewMatrix");
		projectionMatrixUniform = glGetUniformLocationARB(program, "uProjectionMatrix");
		normalMatrixUniform = glGetUniformLocationARB(program, "uNormalMatrix");
		lightPositionUniform = glGetUniformLocationARB(program, "uLightPosition");
		viewPositionUniform = glGetUniformLocationARB(program, "uViewPosition");
	}

	void update() {
		projectionMatrix.setPerspective((float) Math.toRadians(fov), (float) width / height, 0.01f, 100.0f);
		viewPosition.set(10f * (float) Math.cos(rotation), 2f, 10f * (float) Math.sin(rotation));
		viewMatrix.setLookAt(viewPosition.x, viewPosition.y, viewPosition.z, 0f, 0f, 0f, 0f, 1f, 0f);
	}

	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glUseProgramObjectARB(program);
		for (Model.Mesh mesh : model.meshes) {

			glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.vertexArrayBuffer);
			glVertexAttribPointerARB(vertexAttribute, 3, GL_FLOAT, false, 0, 0);

			if (normalAttribute != GL_NO_ERROR) {
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.normalArrayBuffer);
				glVertexAttribPointerARB(normalAttribute, 3, GL_FLOAT, false, 0, 0);
			}

			if (colourAttribute != GL_NO_ERROR) {
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.colourArrayBuffer);
				glVertexAttribPointerARB(colourAttribute, 3, GL_FLOAT, false, 0, 0);
			}

			glUniformMatrix4fvARB(modelMatrixUniform, false, modelMatrix.get(modelMatrixBuffer));
			glUniformMatrix4fvARB(viewMatrixUniform, false, viewMatrix.get(viewMatrixBuffer));
			glUniformMatrix4fvARB(projectionMatrixUniform, false, projectionMatrix.get(projectionMatrixBuffer));

			normalMatrix.set(modelMatrix).invert().transpose();

//			Matrix4f m = new Matrix4f(modelMatrix);
//			m.mul(viewMatrix);
//			normalMatrix.set(m).invert().transpose();
			
			glUniformMatrix3fvARB(normalMatrixUniform, false, normalMatrix.get(normalMatrixBuffer));

			if (lightPositionUniform != GL_NO_ERROR)
				glUniform3fvARB(lightPositionUniform, lightPosition.get(lightPositionBuffer));

			if (viewPositionUniform != GL_NO_ERROR)
				glUniform3fvARB(viewPositionUniform, viewPosition.get(viewPositionBuffer));
			
			glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, mesh.elementArrayBuffer);
			glDrawElements(GL_TRIANGLES, mesh.elementCount, GL_UNSIGNED_INT, 0);
		}
	}

	void loop() {
		while (!glfwWindowShouldClose(window)) {
			glfwPollEvents();
			glViewport(0, 0, fbWidth, fbHeight);
			update();
			render();
			glfwSwapBuffers(window);
		}
	}

	void run() {
		try {
			init();
			loop();
			model.free();
			if (debugProc != null) {
				debugProc.free();
			}
			cpCallback.free();
			keyCallback.free();
			fbCallback.free();
			wsCallback.free();
			glfwDestroyWindow(window);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			glfwTerminate();
		}
	}

	public static void main(String[] args) {
//		 new ObjLoader("blue").run();
//		 new ObjLoader("gouraud").run();
		new ObjLoader("gouraud_lit").run();
	}
}
