package org.andresoviedo.app.model3D.dewu;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.android_3d_model_engine.view.ModelSurfaceView;

import java.net.URI;

public class DewuActivity extends Activity {

    private int TYPE_FILE_OBJ = 0;
    private float[] backgroundColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    private ModelSurfaceView gLView;
    private URI paramUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        this.paramUri = URI.create(b.getString("uri"));


        SceneLoader scene = new SceneLoader(this, paramUri, TYPE_FILE_OBJ, null);
        gLView = new ModelSurfaceView(this, backgroundColor, scene);

        setContentView(gLView);
        scene.setView(gLView);
    }
}
