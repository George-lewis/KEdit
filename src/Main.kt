import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage

// Global App reference
lateinit var app: App

// Application entry point
fun main(args: Array<String>) {

    Application.launch(App::class.java, *args)

}

class App: Application() {

    init {

        app = this

    }


    // Maps controller ID's to windows, scenes, and root nodes
    val windows: MutableMap<Int, Triple<Stage, Scene, Parent>> = mutableMapOf()

    override fun start(stage: Stage?) {

        // Just invoke newWindow with the primary stage
        newWindow(stage)

    }

    /**
     * Sets up and displays a new main window
     */
    fun newWindow(stage: Stage? = Stage()): MainController {

        val stage_ = stage!!

        val loader = FXMLLoader(App::class.java.getResource("MainLayout.fxml"))

        val root: Parent = loader.load()

        val scene = Scene(root)

        scene.stylesheets.add("application.css")

        stage.scene = scene

        stage.width = Config["default_width"]

        stage.height = Config["default_height"]

        stage.minWidth = Config["min_width"]

        stage.minHeight = Config["min_height"]

        stage.icons.add(Image("icon.png"))

        val controller = loader.getController<MainController>()

        controller.controllerID = windows.size

        windows.put(windows.size, Triple(stage_, scene, root))

        stage.show()

        return controller

    }

}