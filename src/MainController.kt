import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.beans.value.ObservableValue
import javafx.event.Event
import javafx.event.EventType
import javafx.fxml.FXML
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.WindowEvent
import javafx.util.Callback
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.properties.Delegates

class MainController {

    companion object Static {

        var paramsParsed = false

        // The file chooser is reusable and can be used across different windows
        val chooser = FileChooser().apply {

            extensionFilters.setAll(
                FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"),
                FileChooser.ExtensionFilter("All Files", "*.*")
            )

            Static.setInitialDirectory(this)

        }

        fun setInitialDirectory(f: FileChooser) {

            val s = Config.get<String>("file_chooser_default_directory").replace("{User}", System.getProperty("user.home"))

            try {

                if (Files.isDirectory(Paths.get(s))) {

                    f.initialDirectory = File(s)

                }

            } catch (e: InvalidPathException) { /* Do not set initial directory */ }

        }

    }

    @FXML lateinit var text: TextArea

    @FXML lateinit var wordCount: Label

    // ### Edit menu items ###
    @FXML lateinit var undo: MenuItem
    @FXML lateinit var redo: MenuItem
    @FXML lateinit var cut: MenuItem
    @FXML lateinit var copy: MenuItem
    @FXML lateinit var paste: MenuItem
    @FXML lateinit var delete: MenuItem

    // ### View menu items ###
    @FXML lateinit var zoomIn: MenuItem
    @FXML lateinit var zoomOut: MenuItem
    @FXML lateinit var fullscreen: MenuItem

    // ### File menu items ###
    @FXML lateinit var newButton: MenuItem
    @FXML lateinit var open: MenuItem
    @FXML lateinit var save: MenuItem
    @FXML lateinit var saveAs: MenuItem
    @FXML lateinit var exit: MenuItem

    // This ID maps the controller to its associated stage, scene, and root
    // This should never be changed, do not touch
    var controllerID: Int by Delegates.notNull()

    // Each controller has an associated stage, scene, and root note
    // These properties allow easy access to the appropriate ones
    val stage: Stage
        get() = app.windows[this.controllerID]!!.stage

    val scene: Scene
        get() = app.windows[this.controllerID]!!.scene

    val root: Parent
        get() = app.windows[this.controllerID]!!.root

    // Each window/controller has its own state
    val state = State()

    // Runs when the .fxml is loaded
    fun initialize() {

        // Bind menuitems to actions
        undo.setOnAction { text.undo() }
        redo.setOnAction { text.redo() }
        cut.setOnAction { text.cut() }
        copy.setOnAction { text.copy() }
        paste.setOnAction { text.paste() }
        delete.setOnAction { text.deleteText(text.selection) }

        zoomIn.setOnAction { text.font = text.font.enlarge(Config["font_change"]) }
        zoomOut.setOnAction { text.font = text.font.shrink(Config["font_change"]) }
        fullscreen.setOnAction { stage.isMaximized = true }
        newButton.setOnAction { new() }
        exit.setOnAction { stage.close() }

        initKeybinds()

        // Set the font of the textarea according to the config
        text.font = Font(Config["font"], Config["font_size"])

        // Controls the word count at the bottom of the window
        // Count is updated each time the text changes
        wordCount.textProperty().bind(
            Bindings.createStringBinding(
                Callable {

                    val n = text.text.numWords()

                    val sn = text.selectedText.numWords()

                    // Returns a string formated "{selected word count}/{total word count} Words"
                    // If there are selected words, else it omits the first part and returns
                    // "{total word count} Words"
                    return@Callable "${if (sn > 0) "${sn}/" else ""}${n} Words"

                },
                text.textProperty(), text.selectedTextProperty()
            ))

        // Set handler for textchange
        text.textProperty().addListener(::textChanged)

        Platform.runLater {

            // Code to be run once everything is set up
            // And lateinit vars (namely: stage, scene, root) are set

            // Binds the title of the window depending on the config string "window_name"
            // This binding recalculates the window name whenever state.fileP or state.changedP changes
            stage.titleProperty().bind(Bindings.createStringBinding(
                Callable {
                    Config.get<String>("window_name")
                    .replace("{File}", state.filename)
                    .replace("{Changed}", if (state.changed && !(Config["autosave"] && state.file != null)) "*" else "")
                },
                state.fileP, state.changedP
            ))

            // Handle window closes
            stage.setOnCloseRequest(::close)

            // If we have not already dealt with the parameters
            if (!MainController.Static.paramsParsed) {

                with(app.parameters.raw) {

                    // If there are parameters
                    if (this.isNotEmpty()) {

                        try {

                            // If the parameter is an actual file
                            if (Files.isRegularFile(Paths.get(first()))) {

                                // Open the file
                                open(File(first()))

                            }

                        } catch (e: InvalidPathException) {

                            // Don't open the argument path

                            print(e)

                        }

                    }

                }

                // We only want to look at the parameters once
                MainController.Static.paramsParsed = true

            }

            scene.accelerators.put(KeyCombination.keyCombination("ctrl+q"), Runnable {

                settings()

            })

        }

    }

    fun disableWindow(enable: Boolean = false) {

        fun disable(n: Node) {

            n.isDisable = !enable

            if (n is Parent) {

                for (child in n.childrenUnmodifiable) {

                    disable(child)

                }

            }

        }

        disable(root)

    }

    // Fires when the text in the edit area is changed
    fun textChanged(obs: ObservableValue<out String>, old: String, new: String) {

        state.changed = true

        if (Config.get<Boolean>("autosave") && state.file != null) {

            // If the user has configured autosave, we save on each keystroke

            save()

        }

    }

    fun restart() {

        val (x, y) = stage.x to stage.y

        val (width, height) = stage.width to stage.height

        stage.onCloseRequest = null

        stage.close()

        app.deregister(this.controllerID)

        app.newWindow().apply {

            state.file?.let {

                this@apply.controller.open(it)

            }

            this@apply.controller.text.text = text.text

            with (this@apply.stage) {

                this.x = x
                this.y = y
                this.width = width
                this.height = height

                show()

            }

        }

    }

    fun settings() {

    app.windows.map { it.key }.forEach {  println("DISABLE WINDOW STUB") /*it.disableWindow()*/ }

        val window = app.newWindow()

        val ncontroller = window.controller

        val nstage = window.stage

        print(nstage)

        Platform.runLater {
            nstage.setOnCloseRequest {

                ncontroller.close(it)

                println("Closed!")

                //close(WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST))

                Config.readConfiguration()

                print(state.file?.name ?: "NULL FILE")

                println(app.windows.size)

                app.windows.forEach { print(it.value.stage.title + " | ")}

                app.windows.map { println() ; it.value.controller }.forEach(MainController::restart)

                setInitialDirectory(chooser)
            }
        }

        ncontroller.newButton.isDisable = true
        ncontroller.newButton.setOnAction {  }

        ncontroller.open.isDisable = true
        ncontroller.open.setOnAction {  }

        ncontroller.open(File("config.txt"))

        nstage.show()

    }

    // Save the text to the file if the file exists
    // Otherwise invoke saveAs()
    fun save() {

        state.file?.let { file ->

            file.writeText(text.text)

            state.changed = false

        } ?: saveAs()

    }

    // Presents a dialogue to the user
    fun saveAs() {

        chooser.showSaveDialog(stage)?.let {

            state.file = it

            save()

        }

    }

    fun changeFont() {

        // This dialog returns a Font instance
        Dialog<Font>().apply {

            initOwner(stage)

            title = "Font selector"

            // These controls allow the user to select what font they want:

            val family = ChoiceBox<String>().apply { items.setAll(Font.getFamilies()); value = text.font.family }

            val size = Spinner<Double>().apply {
                valueFactory = SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 999.0, text.font.size)
            }

            dialogPane.content = HBox(family, size).apply { spacing = 10.0 }

            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)

            resultConverter = Callback {
                return@Callback when (it) {
                    ButtonType.OK -> Font(family.value, size.value)
                    else -> null // The user has cancelled the operation, don't change the font
                }
            }

        }.showAndWait().ifPresent {
            text.font = it // Change the TextArea's font
        }

    }

    fun openDialogue() {

        chooser.showOpenDialog(stage)?.let {

            if (it == state.file) {

                // If the user selected to open the file that's already open, do nothing

                return

            }

            if (Config["open_in_new_window"]) {

                // If the user has configured for 'open' to open in a new window
                // Invoke App::newWindow and instruct the new window to open the file

                val win = app.newWindow()

                win.controller.open(it)

                win.stage.show()

            } else {

                // Logically speaking, the logic when closing the window is the same as when opening a different file
                // We must check that we are not discarding any unsaved changes when we do so
                // So we call this function to ensure as such

                WindowEvent(stage, WindowEvent.ANY).let {

                    close(it)

                    if (it.isConsumed) {

                        // If user selects cancel, do nothing

                        return@openDialogue

                    }

                }

                // open the file

                open(it)

            }

        }

    }

    fun initKeybinds() {

        zoomIn.accelerator = KeyCombination.keyCombination(Config["zoom_in"])
        zoomOut.accelerator = KeyCombination.keyCombination(Config["zoom_out"])
        fullscreen.accelerator = KeyCombination.keyCombination(Config["fullscreen"])

        newButton.accelerator = KeyCombination.keyCombination(Config["new"])
        open.accelerator = KeyCombination.keyCombination(Config["open"])
        save.accelerator = KeyCombination.keyCombination(Config["save"])
        saveAs.accelerator = KeyCombination.keyCombination(Config["save_as"])
        exit.accelerator = KeyCombination.keyCombination(Config["exit"])

    }

    fun open(file: File) {

        if (file != state.file) {

            state.file = file

            text.text = file.readText()

            state.changed = false

        }

    }

    fun new() {

        if (Config["new_in_new_window"]) {

            app.newWindow().stage.show()

        } else {

            // Ensure we're not losing any changes

            WindowEvent(stage, WindowEvent.ANY).let {

                close(it)

                if (it.isConsumed) {

                    // User selected to cancel the operation -> do nothing

                    return@new

                }

            }

            // We are no longer operating on a file
            state.file = null

            // 'File' is blank by default
            text.text = ""

            state.changed = false

        }

    }

    // Opens and about dialogue
    fun about() {

        Alert(Alert.AlertType.INFORMATION).apply {

            initOwner(stage)

            title = "About KEdit"

            headerText = "About KEdit ${Constants.Version}"

            // KEdit graphic
            graphic = ImageView("icon_transparent.png").apply { fitWidth = 64.0; fitHeight = 64.0 }

            // The main content of the dialgoue
            dialogPane.content = TextFlow(

                hyperlink("KEdit", "https://github.com/George-lewis/KEdit"),

                Text("is a simple open source text editor made in"),

                hyperlink("kotlin", "https://kotlinlang.org"),

                Text("and with"),

                hyperlink("JavaFX", "https://openjfx.io/"),

                Text("by"),

                hyperlink("George Lewis", "https://github.com/George-lewis")

            )

        }.show() // We don't need a response from this dialogue

    }

    fun close(e: WindowEvent?) {

        if (state.filename == "config.txt") {

            print("yep....")

        }

        println("closing ${stage.title}")

        if (state.changed) {

            if (Config["save_on_close"] && state.file != null) {

                save()

            } else {

                if (Config["close_warning"]) {

                    Alert(Alert.AlertType.CONFIRMATION).apply {

                        initOwner(stage)

                        title = stage.title

                        headerText = "Do you want to save your changes?"

                        contentText = "You will lose your work otherwise."

                        buttonTypes.setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)


                    }.showAndWait().ifPresent { button ->
                        when(button) {

                            ButtonType.OK -> save()

                            // Cancel close request
                            ButtonType.CANCEL -> e?.consume()

                        }
                    }

                }

            }

        }

        // If there is a window event, and the event's type indicates the window is closing
        // And the user has not cancelled the operation, we must remove this window from
        // the app's registry
        if (e != null && e.eventType == WindowEvent.WINDOW_CLOSE_REQUEST && !e.isConsumed) {

            app.windows.remove(this.controllerID)

        }

    }

}