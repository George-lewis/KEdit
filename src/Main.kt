import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage
import java.net.URL

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

    lateinit var stage: Stage
    lateinit var scene: Scene
    lateinit var root: Pane

    override fun start(stage: Stage?) {

        this.stage = stage!!

        root = FXMLLoader.load(App::class.java.getResource("MainLayout.fxml"))

        scene = Scene(root)

        scene.stylesheets.add("application.css")

        stage.scene = scene

        stage.width = Config["default_width"]

        stage.height = Config["default_height"]

        stage.show()

    }

}