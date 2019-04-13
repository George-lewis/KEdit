import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.beans.value.ObservableValue
import javafx.event.Event
import javafx.fxml.FXML
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.FileChooser
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
import kotlin.reflect.jvm.reflect

class MainController {

    @FXML lateinit var text: TextArea

    @FXML lateinit var wordCount: Label


    // ### Edit menu items ###
    @FXML lateinit var undo: MenuItem
    @FXML lateinit var redo: MenuItem
    @FXML lateinit var cut: MenuItem
    @FXML lateinit var copy: MenuItem
    @FXML lateinit var paste: MenuItem
    @FXML lateinit var delete: MenuItem

    @FXML lateinit var zoomIn: MenuItem
    @FXML lateinit var zoomOut: MenuItem
    @FXML lateinit var fullscreen: MenuItem

    @FXML lateinit var newButton: MenuItem
    @FXML lateinit var open: MenuItem
    @FXML lateinit var save: MenuItem
    @FXML lateinit var saveAs: MenuItem
    @FXML lateinit var exit: MenuItem

    val chooser = FileChooser().apply {

        try {

            val s = Config.get<String>("file_chooser_default_directory").replace("{User}", System.getProperty("user.home"))

            if (Files.isDirectory(Paths.get(s))) {

                initialDirectory = File(s)

            }

        } catch (e: InvalidPathException) { /* Do not set initial directory */ }

        extensionFilters.setAll(
            FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"),
            FileChooser.ExtensionFilter("All Files", "*.*")
        )

    }

    // Runs when the .fxml is loaded
    fun initialize() {

        // Bind menuitems to actions
        undo.setOnAction { text.undo() }
        redo.setOnAction { text.redo() }
        cut.setOnAction { text.cut() }
        copy.setOnAction { text.copy() }
        paste.setOnAction { text.paste() }
        delete.setOnAction { text.deleteText(text.selection) }

        zoomIn.accelerator = KeyCombination.keyCombination(Config["zoom_in"])
        zoomOut.accelerator = KeyCombination.keyCombination(Config["zoom_out"])
        fullscreen.accelerator = KeyCombination.keyCombination(Config["fullscreen"])

        newButton.accelerator = KeyCombination.keyCombination(Config["new"])
        open.accelerator = KeyCombination.keyCombination(Config["open"])
        save.accelerator = KeyCombination.keyCombination(Config["save"])
        saveAs.accelerator = KeyCombination.keyCombination(Config["save_as"])
        exit.accelerator = KeyCombination.keyCombination(Config["exit"])

        zoomIn.setOnAction { text.font = text.font.enlarge(Config["font_change"]) }
        zoomOut.setOnAction { text.font = text.font.shrink(Config["font_change"]) }
        fullscreen.setOnAction { app.stage.isMaximized = true }
        newButton.setOnAction { new() }

        // Set the font of the textarea according to the config
        text.font = Font(Config["font"], Config["font_size"])

        wordCount.textProperty().bind(
            Bindings.createStringBinding(
                Callable { text.text.split(" ", "\t", "\n").filter { it.isNotEmpty() }.size.toString().plus(" Words") },
                text.textProperty()
            ))

        // Set handler for textchange
        text.textProperty().addListener(::textChanged)

        Platform.runLater {

            // Code to be run once everything is set up
            // And lateinit vars (namely: stage, scene, root) are set

            // Binds the title of the window depending on the config string "window_name"
            // This binding recalculates the window name whenever State.fileP or State.changedP changes
            app.stage.titleProperty().bind(Bindings.createStringBinding(
                Callable {
                    Config.get<String>("window_name")
                    .replace("{File}", State.filename)
                    .replace("{Changed}", if (State.changed && !(Config["autosave"] && State.file != null)) "*" else "")
                },
                State.fileP, State.changedP
            ))

            app.stage.setOnCloseRequest(::close)

            with (app.parameters.raw) {

                if (this.isNotEmpty()) {

                    try {

                        if (Files.isRegularFile(Paths.get(first()))) {

                            open(File(first()))

                        }

                    } catch (e: InvalidPathException) {

                        // Don't open the argument path

                        print(e)

                    }

                }

            }

        }

    }

    // Fires when the text in the edit area is changed
    fun textChanged(obs: ObservableValue<out String>, old: String, new: String) {

        State.changed = true

        if (Config.get<Boolean>("autosave") && State.file != null) {

            save()

        }

    }

    // Save the text to the file if the file exists
    // Otherwise invoke saveAs()
    fun save() {

        State.file?.let { file ->

            file.writeText(text.text)

            State.changed = false

        } ?: saveAs()

    }

    // Presents a dialogue to the user
    fun saveAs() {

        chooser.showSaveDialog(app.stage)?.let {

            State.file = it

            save()

        }

    }

    fun changeFont() {

        Dialog<Font>().apply {

            initOwner(app.stage)

            title = "Font selector"

            val family = ChoiceBox<String>().apply { items.setAll(Font.getFamilies()); value = text.font.family }

            val size = Spinner<Double>().apply {
                valueFactory = SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 999.0, text.font.size)
            }

            dialogPane.content = HBox(family, size).apply { spacing = 10.0 }

            dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)

            resultConverter = Callback {
                return@Callback when (it) {
                    ButtonType.OK -> Font(family.value, size.value)
                    else -> null
                }
            }

        }.showAndWait().ifPresent {
            text.font = it
        }

    }

    fun openDialogue() {

        chooser.showOpenDialog(app.stage)?.let { open(it) }

    }

    fun open(file: File) {

        // Logically speaking, the logic when closing the window is the same as when opening a different file
        // We must check that we are not discarding any unsaved changes when we do so
        // So we call this function to ensure as such
        WindowEvent(app.stage, WindowEvent.ANY).let {

            close(it)

            if (it.isConsumed) {

                return@open

            }

        }

        if (Config["open_in_new_window"]) {

            // TODO

        } else {

            State.file = file

            text.text = file.readText()

            State.changed = false

        }

    }

    fun new() {

        ProcessBuilder().command(
            "java",
            File("").canonicalPath,
            "MainKt.class"
        ).inheritIO().start()

    }

    // Opens and about dialogue
    fun about() {

        Alert(Alert.AlertType.INFORMATION).apply {

            initOwner(app.stage)

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

        }.show()

    }

    fun close(e: WindowEvent?) {

        if (State.changed) {

            if (Config["save_on_close"] && State.file != null) {

                save()

            } else {

                if (Config["close_warning"]) {

                    Alert(Alert.AlertType.CONFIRMATION).apply {

                        initOwner(app.stage)

                        title = app.stage.title

                        headerText = "Do you want to save your changes?"

                        contentText = "You will lose your work otherwise."

                        buttonTypes.setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)


                    }.showAndWait().ifPresent { button ->
                        when(button) {

                            ButtonType.OK -> save()

                            ButtonType.CANCEL -> e?.consume()

                        }
                    }

                }

            }

        }

    }

}