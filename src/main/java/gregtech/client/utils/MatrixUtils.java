package gregtech.client.utils;

import mcp.MethodsReturnNonnullByDefault;
import org.lwjgl.util.vector.*;

import javax.annotation.CheckReturnValue;
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
}
