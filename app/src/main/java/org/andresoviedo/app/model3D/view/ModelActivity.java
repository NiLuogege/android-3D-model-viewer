package org.andresoviedo.app.model3D.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.andresoviedo.android_3d_model_engine.camera.CameraController;
import org.andresoviedo.android_3d_model_engine.collision.CollisionController;
import org.andresoviedo.android_3d_model_engine.controller.TouchController;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.android_3d_model_engine.view.ModelRenderer;
import org.andresoviedo.android_3d_model_engine.view.ModelSurfaceView;
import org.andresoviedo.app.model3D.demo.DemoLoaderTask;
import org.andresoviedo.dddmodel2.R;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.event.EventListener;

import java.io.IOException;
import java.net.URI;
import java.util.EventObject;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelActivity extends Activity implements EventListener {

    private static final int REQUEST_CODE_LOAD_TEXTURE = 1000;
    private static final int FULLSCREEN_DELAY = 10000;

    /**
     * Type of model if file name has no extension (provided though content provider)
     * <p>
     * 文件类型  0 = obj, 1 = stl, 2 = dae
     */
    private int paramType;
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
        Log.i("ModelActivity", "Loading activity...");
        super.onCreate(savedInstanceState);

        // Try to get input parameters
        Bundle b = getIntent().getExtras();
        if (b != null) {
            try {
                if (b.getString("uri") != null) {
                    this.paramUri = new URI(b.getString("uri"));
                    Log.i("ModelActivity", "Params: uri '" + paramUri + "'");
                }
                this.paramType = b.getString("type") != null ? Integer.parseInt(b.getString("type")) : -1;
                this.immersiveMode = "true".equalsIgnoreCase(b.getString("immersiveMode"));

                if (b.getString("backgroundColor") != null) {
                    String[] backgroundColors = b.getString("backgroundColor").split(" ");
                    backgroundColor[0] = Float.parseFloat(backgroundColors[0]);
                    backgroundColor[1] = Float.parseFloat(backgroundColors[1]);
                    backgroundColor[2] = Float.parseFloat(backgroundColors[2]);
                    backgroundColor[3] = Float.parseFloat(backgroundColors[3]);
                }
            } catch (Exception ex) {
                Log.e("ModelActivity", "Error parsing activity parameters: " + ex.getMessage(), ex);
            }

        }

        handler = new Handler(getMainLooper());

        // Create our 3D scenario
        Log.i("ModelActivity", "Loading Scene...");
        scene = new SceneLoader(this, paramUri, paramType, gLView);

        //当 传进入文件 为空是，开启loader 去加载 文件
        if (paramUri == null) {
            final LoaderTask task = new DemoLoaderTask(this, null, scene);
            task.execute();
        }

/*        Log.i("ModelActivity","Loading Scene...");
        if (paramUri == null) {
            scene = new ExampleSceneLoader(this);
        } else {
            scene = new SceneLoader(this, paramUri, paramType, gLView);
        }*/

        try {
            Log.i("ModelActivity", "Loading GLSurfaceView...");
            gLView = new ModelSurfaceView(this, backgroundColor, this.scene);
            gLView.addListener(this);

            //将 GLSurfaceView 进行显示
            setContentView(gLView);
            scene.setView(gLView);
        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(this, "Error loading OpenGL view:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        //创建触摸控制器
        try {
            Log.i("ModelActivity", "Loading TouchController...");
            touchController = new TouchController(this);
            touchController.addListener(this);
        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(this, "Error loading TouchController:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        //创建碰撞控制器
        try {
            Log.i("ModelActivity", "Loading CollisionController...");
            collisionController = new CollisionController(gLView, scene);
            collisionController.addListener(scene);
            touchController.addListener(collisionController);
            touchController.addListener(scene);
        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(this, "Error loading CollisionController\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        //创建相机控制器
        try {
            Log.i("ModelActivity", "Loading CameraController...");
            cameraController = new CameraController(scene.getCamera());
            gLView.getModelRenderer().addListener(cameraController);
            touchController.addListener(cameraController);
        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(this, "Error loading CameraController" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        try {
            // TODO: finish UI implementation
            Log.i("ModelActivity", "Loading GUI...");
            gui = new ModelViewerGUI(gLView, scene);
            touchController.addListener(gui);
            gLView.addListener(gui);
            scene.addGUIObject(gui);
        } catch (Exception e) {
            Log.e("ModelActivity", e.getMessage(), e);
            Toast.makeText(this, "Error loading GUI" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Show the Up button in the action bar.
        setupActionBar();

        setupOnSystemVisibilityChangeListener();

        // 加载mode
        // load model
        scene.init();

        Log.i("ModelActivity", "Finished loading");
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        // getActionBar().setDisplayHomeAsUpEnabled(true);
        // }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.model, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setupOnSystemVisibilityChangeListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                // The system bars are visible. Make any desired
                hideSystemUIDelayed();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUIDelayed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.model_toggle_wireframe:
                scene.toggleWireframe();//切换 绘制模式 0：Faces ，1：Wireframe，2：Points，3，Skeleton,4：Normals
                break;
            case R.id.model_toggle_boundingbox:
                scene.toggleBoundingBox(); //是否绘制边框
                break;
            case R.id.model_toggle_skybox: //切换背景
                gLView.toggleSkyBox();
                break;
            case R.id.model_toggle_textures:
                scene.toggleTextures();//切换纹理 （纹理关闭，颜色关闭，纹理开启）
                break;
            case R.id.model_toggle_animation:
                scene.toggleAnimation();//开启或者关闭动画
                break;
            case R.id.model_toggle_smooth:
                scene.toggleSmooth();//动画模型 仅仅 dae 格式可行？
                break;
            case R.id.model_toggle_collision:
                scene.toggleCollision();//碰撞检测.(关闭时 可选中模型，开启时 不能选中但是可以进行操作（比如绘图）)
                break;
            case R.id.model_toggle_lights:
                scene.toggleLighting();//调整灯光模式 rotatingLight 和 drawLighting
                break;
            case R.id.model_toggle_stereoscopic:
                scene.toggleStereoscopic();//切换 3d ，3d 立体浮雕 or not
                break;
            case R.id.model_toggle_blending:
                scene.toggleBlending();//设置是否混合 透明度
                break;
            case R.id.model_toggle_immersive:
                toggleImmersive();//设置是否全屏
                break;
            case R.id.model_load_texture:// 加入纹理（贴纸的感觉）
                Intent target = ContentUtils.createGetContentIntent("image/*");
                Intent intent = Intent.createChooser(target, "Select a file");
                try {
                    startActivityForResult(intent, REQUEST_CODE_LOAD_TEXTURE);
                } catch (ActivityNotFoundException e) {
                    // The reason for the existence of aFileChooser
                }
                break;
        }

        hideSystemUIDelayed();
        return super.onOptionsItemSelected(item);
    }

    private void toggleImmersive() {
        this.immersiveMode = !this.immersiveMode;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        if (this.immersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
        Toast.makeText(this, "Fullscreen " + this.immersiveMode, Toast.LENGTH_SHORT).show();
    }

    private void hideSystemUIDelayed() {
        if (!this.immersiveMode) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::hideSystemUI, FULLSCREEN_DELAY);

    }

    private void hideSystemUI() {
        if (!this.immersiveMode) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hideSystemUIKitKat();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            hideSystemUIJellyBean();
        }
    }

    // This snippet hides the system bars.
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideSystemUIKitKat() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void hideSystemUIJellyBean() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUI() {
        handler.removeCallbacksAndMessages(null);
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_LOAD_TEXTURE:
                // The URI of the selected file
                final Uri uri = data.getData();
                if (uri != null) {
                    Log.i("ModelActivity", "Loading texture '" + uri + "'");
                    try {
                        ContentUtils.setThreadActivity(this);
                        scene.loadTexture(null, uri);
                    } catch (IOException ex) {
                        Log.e("ModelActivity", "Error loading texture: " + ex.getMessage(), ex);
                        Toast.makeText(this, "Error loading texture '" + uri + "'. " + ex
                                .getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        ContentUtils.setThreadActivity(null);
                    }
                }
        }
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
