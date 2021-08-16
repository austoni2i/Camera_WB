package com.app.camerawb.myfilter;

import android.opengl.GLES20;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

/**
 * saturation: The degree of saturation or desaturation to apply to the image (0.0 - 2.0, with 1.0 as the default)
 */
public class GPUImageBlueSaturationFilter extends GPUImageFilter {
    public static final String SATURATION_FRAGMENT_SHADER = "" +
            " varying highp vec2 textureCoordinate;\n" +
            " \n" +
            " uniform sampler2D inputImageTexture;\n" +
            " uniform lowp float saturation;\n" +
            " \n" +
            " // Values from \"Graphics Shaders: Theory and Practice\" by Bailey and Cunningham\n" +
            " const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);\n" +
            " \n" +
            " void main()\n" +
            " {\n" +
            "    lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "    textureColor.b = 10.0;"+
            "    lowp float luminance = dot(textureColor.rgb, luminanceWeighting);\n" +
            "    lowp vec3 greyScaleColor = vec3(luminance);\n" +
            "    \n" +
            "    gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);\n" +
            "     \n" +
            " }";

    private int saturationLocation;
    private float saturation;

    public GPUImageBlueSaturationFilter() {
        this(1.0f);
    }

    public GPUImageBlueSaturationFilter(final float saturation) {
        super(NO_FILTER_VERTEX_SHADER, SATURATION_FRAGMENT_SHADER);
        this.saturation = saturation;
    }

    @Override
    public void onInit() {
        super.onInit();
        saturationLocation = GLES20.glGetUniformLocation(getProgram(), "saturation");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        setSaturation(saturation);
    }

    public void setSaturation(final float saturation) {
        this.saturation = saturation;
        setFloat(saturationLocation, this.saturation);
    }
}