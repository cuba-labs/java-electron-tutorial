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

        Button button = new Button("Hello", event ->
                new Notification("Hello " + nameField.getValue()).show(getPage())
        );

        VerticalLayout content = new VerticalLayout();
        content.addComponents(nameField, button);
        setContent(content);
    }
}