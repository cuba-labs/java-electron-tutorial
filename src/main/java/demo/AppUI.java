package demo;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import java.io.StringReader;

import static com.vaadin.ui.Notification.Type.HUMANIZED_MESSAGE;
import static com.vaadin.ui.Notification.Type.WARNING_MESSAGE;

@Push(transport = Transport.WEBSOCKET)
@Theme(ValoTheme.THEME_NAME)
public class AppUI extends UI {
    @Override
    protected void init(VaadinRequest request) {
        TextField nameField = new TextField();
        nameField.setCaption("Your name");

        Button button = new Button("Print Hello", event -> {
            Runtime runtime = Runtime.getRuntime();

            printHelloDocument(String.format(
                    "Hello %s!\n" +
                            "Your PC is so powerful:\n" +
                            "%s processors\n" +
                            "%s free memory\n" +
                            "%s max memory",
                    nameField.getValue(),
                    runtime.availableProcessors(),
                    runtime.freeMemory(),
                    runtime.maxMemory()));
        });

        VerticalLayout content = new VerticalLayout();
        content.addComponents(nameField, button);
        setContent(content);
    }

    private void printHelloDocument(String value) {
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

        DocFlavor flavor = DocFlavor.READER.TEXT_PLAIN;
        Doc doc = new SimpleDoc(new StringReader(value), flavor, null);

        PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, aset);
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        if (services.length == 0) {
            if (defaultService == null) {
                new Notification("No printer found", WARNING_MESSAGE).show(getPage());
            } else {
                DocPrintJob job = defaultService.createPrintJob();
                printDocument(doc, aset, job);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                PrintService service = ServiceUI.printDialog(null, 200, 200,
                        services, defaultService, flavor, aset);
                if (service != null) {
                    DocPrintJob job = service.createPrintJob();
                    printDocument(doc, aset, job);
                }
            });
        }
    }

    private void printDocument(Doc doc, PrintRequestAttributeSet aset, DocPrintJob job) {
        try {
            job.print(doc, aset);

            getUI().access(() ->
                    new Notification("See the result!", HUMANIZED_MESSAGE)
                            .show(getPage())
            );
        } catch (PrintException e) {
            // may be called from Swing thread
            getUI().access(() -> {
                new Notification("Unable to print file, please check settings",
                        WARNING_MESSAGE).show(getPage());
            });
        }
    }
}