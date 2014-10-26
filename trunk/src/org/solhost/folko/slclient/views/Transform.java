package org.solhost.folko.slclient.views;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

// Inspired by http://goharsha.com/lwjgl-tutorial-series/matrix-transforms/
// and http://goharsha.com/lwjgl-tutorial-series/matrix-projections/

public class Transform {
    public final Matrix4f mat;
    private final FloatBuffer buffer;

    public Transform() {
        buffer = BufferUtils.createFloatBuffer(16);
        mat = new Matrix4f();
    }

    public void reset() {
        mat.setIdentity();
    }

    public Transform translate(float x, float y, float z) {
        Matrix4f.translate(new Vector3f(x, y, z), mat, mat);
        return this;
    }

    public Transform scale(float sx, float sy, float sz) {
        Matrix4f.scale(new Vector3f(sx, sy, sz), mat, mat);
        return this;
    }

    public Transform rotate(float rx, float ry, float rz) {
        Matrix4f.rotate((float) Math.toRadians(rx), new Vector3f(1, 0, 0), mat, mat);
        Matrix4f.rotate((float) Math.toRadians(ry), new Vector3f(0, 1, 0), mat, mat);
        Matrix4f.rotate((float) Math.toRadians(rz), new Vector3f(0, 0, 1), mat, mat);
        return this;
    }

    public Transform combine(Transform other) {
        Transform res = new Transform();
        Matrix4f.mul(other.mat, this.mat, res.mat);
        return res;
    }

    public FloatBuffer getFloatBuffer() {
        buffer.clear();
        mat.store(buffer);
        buffer.rewind();
        return buffer;
    }

    public static Transform perspective(float fov, float aspectRatio, float zNear, float zFar) {
        Transform t = new Transform();

        float yScale = 1f / (float) Math.tan(Math.toRadians(fov / 2.0f));
        float xScale = yScale / aspectRatio;
        float frustumLen = zFar - zNear;
        t.mat.m00 = xScale;
        t.mat.m11 = yScale;
        t.mat.m22 = -((zFar + zNear) / frustumLen);

        t.mat.m23 = -1;
        t.mat.m32 = -((2 * zFar * zNear) / frustumLen);
        t.mat.m33 = 0;
        return t;
    }

    public static Transform orthographic(float left, float top, float right, float bottom, float zNear, float zFar) {
        Transform t = new Transform();
        // Scale so that (left, top, near) is mapped to (-1, -1, 1) and (right, bottom, far) to (1, 1, -1)
        t.mat.m00 = 2 / (right - left);
        t.mat.m11 = 2 / (top - bottom);
        t.mat.m22 = -2 / (zFar - zNear);
        return t;
    }

    public static Transform UO(float t, float pc) {
        Transform res = new Transform();
        res.mat.m00 =  t;
        res.mat.m01 =  t;
        res.mat.m10 = -t;
        res.mat.m11 =  t;
        res.mat.m21 = -pc;
        return res;
    }
}
