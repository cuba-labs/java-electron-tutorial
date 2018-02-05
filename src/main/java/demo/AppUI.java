package demo;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

@Push(transport = Transport.WEBSOCKET)
@Theme(ValoTheme.THEME_NAME)
public class AppUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        TextField nameField = new TextField();
        nameField.setCaption("Your name");

        Button button = new Button("Print Hello", event -> {
            printHelloDocument(nameField.getValue());
        });

        VerticalLayout content = new VerticalLayout();
        content.addComponents(nameField, button);
        setContent(content);
    }

    private void printHelloDocument(String value) {
        new Notification("Hi " + value).show(getPage());
    }
}