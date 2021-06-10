package org.andresoviedo.app.model3D.dewu;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.andresoviedo.android_3d_model_engine.camera.CameraController;
import org.andresoviedo.android_3d_model_engine.collision.CollisionController;
import org.andresoviedo.android_3d_model_engine.controller.TouchController;
import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.android_3d_model_engine.view.ModelRenderer;
import org.andresoviedo.android_3d_model_engine.view.ModelSurfaceView;
import org.andresoviedo.app.model3D.view.ModelViewerGUI;
import org.andresoviedo.util.event.EventListener;

import java.net.URI;
import java.util.EventObject;

public class DewuActivity extends Activity implements EventListener {

    private int TYPE_FILE_OBJ = 0;
    private float[] backgroundColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    private ModelSurfaceView gLView;
    private URI paramUri;
    private TouchController touchController;
    private ModelViewerGUI gui;
    private CollisionController collisionController;
    private CameraController cameraController;

    private void log(String content) {
        Log.e("DewuActivity", content);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        this.paramUri = URI.create(b.getString("uri"));

        log(paramUri.toString());


        SceneLoader scene = new SceneLoader(this, paramUri, TYPE_FILE_OBJ, gLView);
        gLView = new ModelSurfaceView(this, backgroundColor, scene);

        setContentView(gLView);
        scene.setView(gLView);
//
//
////创建触摸控制器
//        touchController = new TouchController(this);
//        touchController.addListener(this);
//
////创建碰撞控制器
//        collisionController = new CollisionController(gLView, scene);
//        collisionController.addListener(scene);
//        touchController.addListener(collisionController);
//        touchController.addListener(scene);
//
////创建相机控制器
//        cameraController = new CameraController(scene.getCamera());
//        gLView.getModelRenderer().addListener(cameraController);
//        touchController.addListener(cameraController);
//
//        gui = new ModelViewerGUI(gLView, scene);
//        touchController.addListener(gui);
//        gLView.addListener(gui);
//        scene.addGUIObject(gui);


        scene.init();
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ModelRenderer.ViewEvent) {
            ModelRenderer.ViewEvent viewEvent = (ModelRenderer.ViewEvent) event;
            if (viewEvent.getCode() == ModelRenderer.ViewEvent.Code.SURFACE_CHANGED) {
                touchController.setSize(viewEvent.getWidth(), viewEvent.getHeight());
                gLView.setTouchController(touchController);

                // process event in GUI
                if (gui != null) {
                    gui.setSize(viewEvent.getWidth(), viewEvent.getHeight());
                    gui.setVisible(true);
                }
            }
        }
        return true;
    }
}
