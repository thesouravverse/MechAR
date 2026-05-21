package com.thesouravverse.mechar.ui

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
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

/** Base geometry size in metres. The cube is created at 1 m and then
 *  scaled at runtime via [Node.scale] so resize is cheap and live. */
private val BASE_CUBE = Float3(1f, 1f, 1f)

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
    var mechCubeNode by remember { mutableStateOf<Node?>(null) }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }

    // Live-resize: whenever the requested size changes, rescale the cube.
    LaunchedEffect(state.cubeSizeCm) {
        mechCubeNode?.let { applySize(it, state.cubeSizeCm) }
    }

    // Despawn the mech node if game state says it's gone (e.g. killed).
    LaunchedEffect(state.mechPlaced) {
        if (!state.mechPlaced) {
            mechAnchorNode?.let { node ->
                childNodes.remove(node)
                node.destroy()
            }
            mechAnchorNode = null
            mechCubeNode = null
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

                    val mechVisual = createMechShape(
                        engine = engine,
                        materialLoader = materialLoader,
                        shape = state.shape
                    )
                    applySize(mechVisual, state.cubeSizeCm)
                    anchorNode.addChildNode(mechVisual)
                    childNodes.add(anchorNode)
                    mechAnchorNode = anchorNode
                    mechCubeNode = mechVisual
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

/** Spawn the requested primitive. All shapes are built at 1 m and scaled at runtime. */
private fun createMechShape(
    engine: Engine,
    materialLoader: MaterialLoader,
    shape: MechShape
): Node {
    val mat = materialLoader.createColorInstance(
        color = dev.romainguy.kotlin.math.Float4(0.85f, 0.18f, 0.32f, 1f),
        metallic = 0.6f,
        roughness = 0.35f,
        reflectance = 0.5f
    )
    return when (shape) {
        MechShape.CUBE -> CubeNode(
            engine = engine,
            size = BASE_CUBE,
            center = Float3(0f, BASE_CUBE.y / 2f, 0f), // sit on floor
            materialInstance = mat
        )
        MechShape.SPHERE -> SphereNode(
            engine = engine,
            radius = 0.5f,
            center = Float3(0f, 0.5f, 0f), // bottom of sphere touches floor
            materialInstance = mat
        )
        MechShape.CYLINDER -> CylinderNode(
            engine = engine,
            radius = 0.5f,
            height = 1f,
            center = Float3(0f, 0.5f, 0f), // sit on floor
            materialInstance = mat
        )
    }
}

/** Apply a uniform scale to the cube so its world size is [sizeCm] cm. */
private fun applySize(node: Node, sizeCm: Int) {
    val s = sizeCm / 100f
    node.scale = Float3(s, s, s)
}
