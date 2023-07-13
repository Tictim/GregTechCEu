package gregtech.client.utils;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.util.vector.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MatrixUtils {

    private MatrixUtils() {}

    @CheckReturnValue
    public static Vector3f transform(Matrix4f matrix, Vector3f vector) {
        Vector3f vec = new Vector3f();
        transform(matrix, vector, vec);
        return vec;
    }

    public static void transform(Matrix4f matrix, Vector3f vector, Vector3f dest) {
        Vector4f temp = new Vector4f(vector.x, vector.y, vector.z, 1f);
        Matrix4f.transform(matrix, temp, temp);
        float scale = 1f / temp.w;
        dest.set(temp.x * scale, temp.y * scale, temp.z * scale);
    }

    @CheckReturnValue
    public static Vector2f transform(Matrix3f matrix, Vector2f vector) {
        Vector2f vec = new Vector2f();
        transform(matrix, vector, vec);
        return vec;
    }

    public static void transform(Matrix3f matrix, Vector2f vector, Vector2f dest) {
        Vector3f temp = new Vector3f(vector.x, vector.y, 1f);
        Matrix3f.transform(matrix, temp, temp);
        float scale = 1f / temp.z;
        dest.set(temp.x * scale, temp.y * scale);
    }

    @CheckReturnValue
    public static Vector3f transform(javax.vecmath.Matrix4f matrix, Vector3f vector) {
        Vector3f vec = new Vector3f();
        transform(matrix, vector, vec);
        return vec;
    }

    public static void transform(javax.vecmath.Matrix4f matrix, Vector3f vector, Vector3f dest) {
        javax.vecmath.Vector4f iHateJavaxVecmath = new javax.vecmath.Vector4f(vector.x, vector.y, vector.z, 1);
        matrix.transform(iHateJavaxVecmath);
        float scale = 1f / iHateJavaxVecmath.w;
        dest.set(iHateJavaxVecmath.x * scale, iHateJavaxVecmath.y * scale, iHateJavaxVecmath.z * scale);
    }

    public static void translate(Matrix3f matrix, float x, float y) {
        if (x == 0 && y == 0) return;
        Matrix3f m2 = new Matrix3f();
        m2.m02 = x;
        m2.m12 = y;
        Matrix3f.mul(matrix, m2, matrix);
    }

    public static void scale(Matrix3f matrix, float scale) {
        if (scale == 1) return;
        scale(matrix, scale, scale);
    }

    public static void scale(Matrix3f matrix, float x, float y) {
        if (x == 1 && y == 1) return;
        Matrix3f m2 = new Matrix3f();
        m2.m00 = x;
        m2.m11 = y;
        Matrix3f.mul(matrix, m2, matrix);
    }

    public static void rotate(Matrix3f matrix, float radians) {
        float cos = (float) Math.cos(radians);
        if (cos == 1) return;
        float sin = (float) Math.sin(radians);
        Matrix3f m2 = new Matrix3f();
        m2.m00 = cos;
        m2.m01 = -sin;
        m2.m10 = sin;
        m2.m11 = cos;
        Matrix3f.mul(matrix, m2, matrix);
    }

    public static Matrix4f fromQuaternion(Quaternion quat) {
        float xx = quat.x * quat.x;
        float xy = quat.x * quat.y;
        float xz = quat.x * quat.z;
        float xw = quat.x * quat.w;
        float yy = quat.y * quat.y;
        float yz = quat.y * quat.z;
        float yw = quat.y * quat.w;
        float zz = quat.z * quat.z;
        float zw = quat.z * quat.w;
        Matrix4f mat = new Matrix4f();
        mat.m00 = 1 - 2 * (yy + zz);
        mat.m01 = 2 * (xy + zw);
        mat.m02 = 2 * (xz - yw);
        mat.m03 = 0;
        mat.m10 = 2 * (xy - zw);
        mat.m11 = 1 - 2 * (xx + zz);
        mat.m12 = 2 * (yz + xw);
        mat.m13 = 0;
        mat.m20 = 2 * (xz + yw);
        mat.m21 = 2 * (yz - xw);
        mat.m22 = 1 - 2 * (xx + yy);
        mat.m23 = 0;
        mat.m30 = 0;
        mat.m31 = 0;
        mat.m32 = 0;
        mat.m33 = 1;
        return mat;
    }

    public static void translate(Matrix4f mat, float x, float y, float z) {
        Matrix4f.translate(new Vector3f(x, y, z), mat, mat);
    }

    public static void scale(Matrix4f mat, float scale) {
        scale(mat, scale, scale, scale);
    }

    public static void scale(Matrix4f mat, float x, float y, float z) {
        Matrix4f.scale(new Vector3f(x, y, z), mat, mat);
    }

    // TODO document???
    // stole from ItemCameraTransforms, idk which angle is rotated first lmao
    public static void rotate(Matrix4f mat, float xDegrees, float yDegrees, float zDegrees) {
        final float toRads = (float) (Math.PI / 180);

        float xRads = xDegrees * toRads;
        float yRads = yDegrees * toRads;
        float zRads = zDegrees * toRads;
        float xSin = MathHelper.sin(.5f * xRads);
        float xCos = MathHelper.cos(.5f * xRads);
        float ySin = MathHelper.sin(.5f * yRads);
        float yCos = MathHelper.cos(.5f * yRads);
        float zSin = MathHelper.sin(.5f * zRads);
        float zCos = MathHelper.cos(.5f * zRads);
        Quaternion q = new Quaternion(
                xSin * yCos * zCos + xCos * ySin * zSin,
                xCos * ySin * zCos - xSin * yCos * zSin,
                xSin * ySin * zCos + xCos * yCos * zSin,
                xCos * yCos * zCos - xSin * ySin * zSin);
        Matrix4f.mul(mat, fromQuaternion(q), mat);
    }

    @Nullable
    public static javax.vecmath.Matrix4f toVecmath(@Nullable Matrix4f matrix) {
        return matrix == null ? null : new javax.vecmath.Matrix4f(
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33
        );
    }
}
