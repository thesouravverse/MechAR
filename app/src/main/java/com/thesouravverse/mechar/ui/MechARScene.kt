package com.thesouravverse.mechar.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.filament.Engine
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

private const val TAG = "MechARScene"

/** Asset path inside `app/src/main/assets/`. If the file is missing the
 *  scene falls back to a coloured cube so the app still works. */
private const val MECH_MODEL_PATH = "models/mech.glb"

/** Physical size (metres) used for the fallback cube AND as the
 *  authoritative scale of the mech.  ~30 cm wide × 70 cm tall. */
private val MECH_SIZE = Float3(0.3f, 0.7f, 0.3f)

@Composable
fun MechARScene(
    state: GameUiState,
    onMechPlaced: () -> Unit,
    onMechTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()

    var frame by remember { mutableStateOf<Frame?>(null) }
    var planeRenderer by remember { mutableStateOf(true) }
    var mechAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }

    // Despawn the mech node if game state says it's gone (e.g. killed).
    LaunchedEffect(state.mechPlaced) {
        if (!state.mechPlaced) {
            mechAnchorNode?.let { node ->
                childNodes.remove(node)
                node.destroy()
            }
            mechAnchorNode = null
            planeRenderer = true
        }
    }

    ARScene(
        modifier = modifier,
        childNodes = childNodes,
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        collisionSystem = collisionSystem,
        sessionConfiguration = { session, config ->
            config.depthMode =
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC
                else
                    Config.DepthMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        cameraNode = cameraNode,
        planeRenderer = planeRenderer,
        onTrackingFailureChanged = { reason -> trackingFailure = reason },
        onSessionUpdated = { _, updatedFrame -> frame = updatedFrame },
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { motionEvent, tappedNode ->

                // 1. If they tapped on the mech (or anything parented under it) → shoot
                if (tappedNode != null && tappedNode.belongsTo(mechAnchorNode)) {
                    onMechTapped()
                    return@rememberOnGestureListener
                }

                // 2. Otherwise, if no mech yet, try to deploy on a detected plane
                if (mechAnchorNode == null) {
                    val anchor = frame
                        ?.hitTest(motionEvent.x, motionEvent.y)
                        ?.firstOrNull { hit -> hit.isValid(depthPoint = false, point = false) }
                        ?.createAnchorOrNull()
                        ?: return@rememberOnGestureListener

                    val anchorNode = AnchorNode(engine, anchor).apply {
                        isEditable = false
                    }

                    val mechVisual = createMechVisual(
                        engine = engine,
                        modelLoader = modelLoader,
                        materialLoader = materialLoader
                    )
                    anchorNode.addChildNode(mechVisual)
                    childNodes.add(anchorNode)
                    mechAnchorNode = anchorNode
                    planeRenderer = false
                    onMechPlaced()
                }
            }
        )
    )
}

/** Walk up the parent chain to see if [this] descends from [target]. */
private fun Node.belongsTo(target: Node?): Boolean {
    if (target == null) return false
    var cur: Node? = this
    while (cur != null) {
        if (cur === target) return true
        cur = cur.parent
    }
    return false
}

/** Try to load the mech glb; fall back to a coloured cube so v1 ships
 *  even before you drop a real model into assets/. */
private fun createMechVisual(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader
): Node {
    return runCatching {
        val instance = modelLoader.createModelInstance(MECH_MODEL_PATH)
        ModelNode(
            modelInstance = instance,
            scaleToUnits = MECH_SIZE.y // scale so model's biggest dim ≈ mech height
        ).apply {
            isEditable = false
        } as Node
    }.getOrElse { t ->
        Log.w(TAG, "mech.glb not found, falling back to cube: ${t.message}")
        val mat = materialLoader.createColorInstance(
            color = dev.romainguy.kotlin.math.Float4(0.85f, 0.18f, 0.32f, 1f),
            metallic = 0.6f,
            roughness = 0.35f,
            reflectance = 0.5f
        )
        CubeNode(
            engine = engine,
            size = MECH_SIZE,
            center = Float3(0f, MECH_SIZE.y / 2f, 0f), // sit on the floor
            materialInstance = mat
        )
    }
}
