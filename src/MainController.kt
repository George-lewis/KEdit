import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableStringValue
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TextArea
import javafx.scene.input.*
import javafx.scene.text.Font
import javafx.stage.StageStyle
import javafx.stage.WindowEvent
import java.io.File

class MainController {

    @FXML lateinit var text: TextArea

    // Runs when the .fxml is loaded
    fun initialize() {

        // Set the font of the textarea according to the config
        text.font = Font(Config["font"], Config["font_size"])

        // Set handler for textchange
        text.textProperty().addListener(::textChanged)

        Platform.runLater {

            // Code to be run once everything is set up
            // And lateinit vars (namely: stage, scene, root) are set

            // Binds the title of the window depending on the config string "window_name"
            // This binding recalculates the window name whenever State.fileP or State.changedP changes
            app.stage.titleProperty().bind(Bindings.createStringBinding(
                callable {
                    Config.get<String>("window_name")
                    .replace("{File}", State.filename)
                    .replace("{Changed}", if (State.changed) "*" else "")
                },
                State.fileP, State.changedP
            ))

            app.stage.setOnCloseRequest(::close)

        }

    }

    fun textChanged(obs: ObservableValue<out String>, old: String, new: String) {

        State.changed = true

    }

    fun save() {

    }

    fun saveAs() {

    }

    fun close(e: WindowEvent?) {

        if (State.changed) {

            if (State.file != null) {

                if (Config["save_on_close"]) {

                    save()

                }

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