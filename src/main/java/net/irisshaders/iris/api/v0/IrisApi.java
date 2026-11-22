package net.irisshaders.iris.api.v0;

public interface IrisApi {
    static IrisApi getInstance() {
        throw new UnsupportedOperationException("Compile-time stub of the Iris API");
    }

    boolean isShaderPackInUse();
}
