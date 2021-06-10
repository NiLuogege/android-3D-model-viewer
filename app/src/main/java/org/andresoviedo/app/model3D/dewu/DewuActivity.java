package org.andresoviedo.app.model3D.dewu;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.andresoviedo.android_3d_model_engine.camera.CameraController;
import org.andresoviedo.android_3d_model_engine.collision.CollisionController;
import org.andresoviedo.android_3d_model_engine.controller.TouchController;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.android_3d_model_engine.view.ModelRenderer;
import org.andresoviedo.android_3d_model_engine.view.ModelSurfaceView;
import org.andresoviedo.app.model3D.demo.DemoLoaderTask;
import org.andresoviedo.app.model3D.view.ModelViewerGUI;
import org.andresoviedo.dddmodel2.R;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.event.EventListener;

import java.io.IOException;
import java.net.URI;
import java.util.EventObject;

public class DewuActivity extends Activity implements EventListener {

    private static final int REQUEST_CODE_LOAD_TEXTURE = 1000;
    private static final int FULLSCREEN_DELAY = 10000;

    /**
     * Type of model if file name has no extension (provided though content provider)
     * <p>
     * 文件类型  0 = obj, 1 = stl, 2 = dae
     */
    private int paramType=0;
    /**
     * The file to load. Passed as input parameter
     */
    private URI paramUri;
    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     * 是否全屏模式
     */
    private boolean immersiveMode;
    /**
     * Background GL clear color. Default is light gray
     * 设置背景色 RGBA
     */
    private float[] backgroundColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    private ModelSurfaceView gLView;
    private TouchController touchController;
    private SceneLoader scene;
    private ModelViewerGUI gui;
    private CollisionController collisionController;


    private Handler handler;
    private CameraController cameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        this.paramUri = URI.create(b.getString("uri"));

        scene = new SceneLoader(this, paramUri, paramType, gLView);

        gLView = new ModelSurfaceView(this, backgroundColor, this.scene);
        gLView.addListener(this);

        //将 GLSurfaceView 进行显示
        setContentView(gLView);
        scene.setView(gLView);

        //创建触摸控制器
        touchController = new TouchController(this);
        touchController.addListener(this);

        //创建碰撞控制器
        collisionController = new CollisionController(gLView, scene);
        collisionController.addListener(scene);
        touchController.addListener(collisionController);
        touchController.addListener(scene);

        //创建相机控制器
        cameraController = new CameraController(scene.getCamera());
        gLView.getModelRenderer().addListener(cameraController);
        touchController.addListener(cameraController);

        gui = new ModelViewerGUI(gLView, scene);
        touchController.addListener(gui);
        gLView.addListener(gui);
        scene.addGUIObject(gui);

        // 加载mode
        // load model
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
