# Java Electron Tutorial

In this short enough tutorial I will show you how to craft your own Java Desktop toolkit on top of Electron, Jetty and Vaadin.

## Getting started

We will start with Gradle build system and Node.js installation. Download and install the latest stable version of Gradle: https://gradle.org/releases/ and Node.js: https://nodejs.org/en/download/

Create a new directory and run using command line in this directory:

> gradle init --type java-application

Gradle will produce a set of project stub files. Now, you can easily open the directory using Intellij Idea or Eclipse as Java project.

Let’s remove `src/main/java/App.java` and `src/test/java/AppTest.java` files and open `build.gradle` file.

Modify build.gradle file to match with the following:
```java
apply plugin: 'java'

repositories {
    jcenter()
}

dependencies {
}
```

## Vaadin UI at the speed of light

Previously, I’ve already told you about our experience with Vaadin in the blog post: https://vaadin.com/blog/cuba-studio-how-we-use-vaadin-for-our-web-development-tool

For me, it is battle-proven Java framework that enables us to build complex UI without a single line of HTML and JS. Let’s employ it as a basis for our UI.

I will create a simple Vaadin application from scratch. First, we need to add necessary dependencies to build.gradle script, enable `war` and `gretty` plugins:
```java
plugins {
   id 'org.akhikhl.gretty' version '2.0.0'
}

apply plugin: 'java'
apply plugin: 'war'

repositories {
   jcenter()
}

dependencies {
   compile 'javax.servlet:javax.servlet-api:3.0.1'
   compile 'com.vaadin:vaadin-server:8.1.6'
   compile 'com.vaadin:vaadin-push:8.1.6'
   compile 'com.vaadin:vaadin-client-compiled:8.1.6'
   compile 'com.vaadin:vaadin-themes:8.1.6'
}

gretty {
   contextPath = '/app'
}
```

Refresh your Gradle project in IDE and you will be able to create UI using Vaadin.

Let’s build Hello World on Vaadin. Simply create package demo and Java class AppUI inside of it.

```java
@Theme(ValoTheme.THEME_NAME)
public class AppUI extends UI {
   @Override
   protected void init(VaadinRequest request) {
       TextField nameField = new TextField();
       nameField.setCaption("Your name");

       Button button = new Button("Hello", event ->
               new Notification(
                   "Hello " + nameField.getValue()
               ).show(getPage())
       );

       VerticalLayout content = new VerticalLayout();
       content.addComponents(nameField, button);
       setContent(content);
   }
}
```

As you see, we implement UI using Java API. After that, we define servlet class `demo.AppServlet`:

```java
@WebServlet(urlPatterns = "/*", name = "AppServlet")
@VaadinServletConfiguration(ui = AppUI.class, productionMode = false)
public class AppServlet extends VaadinServlet {
}
```

Finally, build and start the app using gradle:

> gradle assemble jettyStart

Open http://localhost:8080/app in your favorite web browser. That was easy!

![Vaadin UI](/images/image1.png)

At the moment we have a pretty standard web application, it can be deployed to server or we can give it to Desktop users along with a servlet container (Tomcat, for instance) and make them use it from a web browser.

## How to embed Jetty into Java applications

We will gradually transform our application into Desktop form. First step - get rid of WAR and external servlet container.

Modify `build.gradle` file:

```java
apply plugin: 'java'
apply plugin: 'application'

repositories {
   jcenter()
}

dependencies {
   compile 'javax.servlet:javax.servlet-api:3.0.1'
   compile 'com.vaadin:vaadin-server:8.1.6'
   compile 'com.vaadin:vaadin-push:8.1.6'
   compile 'com.vaadin:vaadin-client-compiled:8.1.6'
   compile 'com.vaadin:vaadin-themes:8.1.6'

   compile 'org.eclipse.jetty:jetty-server:9.3.20.v20170531'
   compile 'org.eclipse.jetty:jetty-webapp:9.3.20.v20170531'
   compile 'org.eclipse.jetty:jetty-continuation:9.3.20.v20170531'
}

applicationName = 'demo'
mainClassName = 'demo.Launcher'
```

I’ve added jetty jars to the project dependencies and replaced `war` and `gretty` plugins with `application` plugin. The only thing left to do is to implement `demo.Launcher` class.

That’s quite an easy task because the process of Jetty embedding is already described in the official manual: http://www.eclipse.org/jetty/documentation/current/embedding-jetty.html

Thus, our Launcher will look as follows:
```java
public class Launcher {
   public static void main(String[] args) {
       System.out.println("Server starting...");

       ServletContextHandler contextHandler =
               new ServletContextHandler(null, "/", true, false);
       contextHandler.setSessionHandler(new SessionHandler());
       contextHandler.addServlet(new ServletHolder(AppServlet.class), "/*");

       Server embeddedServer = new Server(8080);
       embeddedServer.setHandler(contextHandler);

       try {
           embeddedServer.start();
           embeddedServer.join();
       } catch (Exception e) {
           System.err.println("Server error:\n" + e);
       }
       System.out.println("Server stopped");
   }
}
```

Now, we will be able to start our application as a single executable without external web server applications:

> gradle run

The application will be accessible on http://localhost:8080. Moreover, we can build it to a single ZIP archive with all the dependencies and distribute it to our users:

> gradle distZip

## Simple Electron application

A basic Electron app consists of three files: `package.json` (metadata), `main.js` (code) and `index.html` (graphical user interface). The framework is provided by the Electron executable file (electron.exe in Windows, electron.app on macOS and electron on Linux).

At this stage, we will create simple electron application without our server side using Electron quick start guide: https://github.com/electron/electron/blob/master/docs/tutorial/quick-start.md. First, create `src/main/electron/package.json` file:

```json
{
 "name"    : "demo-app",
 "version" : "0.1.0",
 "main"    : "main.js"
}
```

We will show the stub HTML page `src/main/electron/index.html`:

```html
<h1>
   Hello world!
</h1>
```

As it is described in Electron quick start, we will use the following JS code in `src/main/electron/main.js`:

```javascript
const {app, BrowserWindow} = require('electron');
const path = require('path');
const url = require('url');

let win;

function createWindow() {
   win = new BrowserWindow({width: 800, height: 600});

   win.loadURL(url.format({
       pathname: path.join(__dirname, 'index.html'),
       protocol: 'file:',
       slashes: true
   }));

   win.on('closed', () => {
       win = null
   })
}

app.on('ready', createWindow);

app.on('window-all-closed', () => {
   if (process.platform !== 'darwin') {
       app.quit()
   }
});

app.on('activate', () => {
   if (win === null) {
       createWindow()
   }
});
```

Now, we are ready to install electron using NPM. Go to `src/main/electron` directory and execute the following command:

> npm install electron --save-dev

NPM will download and install electron to your PC. Let’s start it!

> npx electron

![Electron UI](/images/image2.png)

At the moment, we've got all the pieces of the puzzle sitting right there on the table. Now all we have to do is put them in the right order.

## Bring all together

First, simply remove src/main/electron/index.html file. We will open our application UI right on the application start.

Then, build the application using Gradle and install it to the `build/install` directory:

> gradle installDist

Copy `build/install/demo` directory into `src/main/electron/demo`. Add +x permission for `demo/bin/demo` file if you use Mac OS or Linux.

The hardest part is to start a Java process from Electron runtime and maintain the consistent state of the Java executable and a browser window. It can be done using child_process subsystem of Node.js: https://nodejs.org/api/child_process.html The startup of the application server process should look as follows:

```javascript
platform = process.platform;

// Check operating system
if (platform === 'win32') {
   serverProcess = require('child_process')
       .spawn('cmd.exe', ['/c', 'demo.bat'],
           {
               cwd: app.getAppPath() + '/demo/bin'
           });
} else {
   serverProcess = require('child_process')
       .spawn(app.getAppPath() + '/demo/bin/demo');
}

let appUrl = 'http://localhost:8080';

const openWindow = function () {
   mainWindow = new BrowserWindow({
       title: 'Demo',
       width: 640,
       height: 480
   });

   mainWindow.loadURL(appUrl);

   mainWindow.on('closed', function () {
       mainWindow = null;
   });

   mainWindow.on('close', function (e) {
       if (serverProcess) {
           e.preventDefault();
           // kill Java executable
       }
   });
};

const startUp = function () {
   const requestPromise = require('minimal-request-promise');

   requestPromise.get(appUrl).then(function (response) {
       console.log('Server started!');
       openWindow();
   }, function (response) {
       console.log('Waiting for the server start...');

       setTimeout(function () {
           startUp();
       }, 200);
   });
};

startUp();
```

Here we use the `minimal-request-promise` package to check if an application has started, install it using NPM (we call npm/npx commands from `src/main/electron` directory):

> npm install minimal-request-promise

In order to stop the Java part, we will use `tree-kill` package. Install it:

> npm install tree-kill

In the mainWindow close callback we should kill the server process:

```javascript
// kill Java executable
const kill = require('tree-kill');
kill(serverProcess.pid, 'SIGTERM', function () {
   console.log('Server process killed');

   serverProcess = null;

   mainWindow.close();
});
```

Finally, we can start the application:

> npx electron .

![Application](/images/image3.png)

It is alive!

In fact, any Java application can be started using this approach, you can run your existing Spring Boot application or tomcat with a deployed WAR file. There is no limitation to use only Vaadin! Feel free to start your favorite backend!

## Periphery integration

Well, it seems that this really simple example does work, but how we can employ peripheral devices or communicate with OS?

Since we have full-featured Java process, we can easily write/read local files and use all the features of OS. For instance, let’s print OS info to a local printer.

1) First, we create a text document with OS information

```java
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
```

2) In Java, we have access to the special `PrintServiceLookup` class that enables us to query for available printers and send documents to the queue.

```java
PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

DocFlavor flavor = DocFlavor.READER.TEXT_PLAIN;
Doc doc = new SimpleDoc(new StringReader(value), flavor, null);

PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, aset);
PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
```

3) We either send document to default printer or let a user to decide which one to use.

```java
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
```

4) Finally, the document printing method will be:

```java
private void printDocument(Doc doc, PrintRequestAttributeSet aset,
                           DocPrintJob job) {
   try {
       job.print(doc, aset);
       getUI().access(() ->
               new Notification(
                   "See the result!", HUMANIZED_MESSAGE
               ).show(getPage())
       );
   } catch (PrintException e) {
       // can be called from Swing thread
       getUI().access(() -> {
           new Notification(
               "Unable to print file, please check settings",
               WARNING_MESSAGE
           ).show(getPage());
       });
   }
}
```

Moreover, there are well-known APIs in Java for calling functions from native libraries, such as JNI or JNA. Thus, there are no restrictions for our application in comparison with web-only apps.

Implementation of the offline mode for this application essentially the same as for any Desktop application - cache data locally using an embedded database, e.g. HSQL, route business logic calls to local data in case of unavailable network, and voila!

The full code of the tutorial is available on GitHub: https://github.com/cuba-labs/java-electron-tutorial

## Tips and Tricks

There are several ways to improve our solution.

### Implement two-way communication between Electron UI code and Vaadin

Vaadin allows you to call JavaScript functions from Java and expose Java methods as JavaScript API. It is enough to implement simple communication bus between BrowserWindow and Java code. For instance, you will be able to use Electron native menus and Desktop notifications in operating systems.

Suppose, we want to use native menu File - Exit that will notify Java application before exit. We need to expose Java API:

```java
JavaScript js = getPage().getJavaScript();
js.addFunction("appWindowExit", arguments -> onWindowExit());
```

From Electron we can call it using BrowserWindow object:

```javascript
mainWindow.webContents.executeJavaScript("appWindowExit();");
```

The opposite call is also possible. Simply use JavaScipt object from Vaadin:

```java
private void callElectronUiApi(String[] args) {
   JsonArray paramsArray = Json.createArray();
   int i = 0;
   for (String arg : args) {
       paramsArray.set(i, Json.create(arg));
       i++;
   }
   getPage().getJavaScript().execute(
     "callElectronUiApi(" + paramsArray.toJson() + ")"
   );
}
```

### Use WebSocket for UI to speed up communication and strip useless HTTP headers.

Each time our application handles a user event, it sends and receives HTTP headers. They are almost useless in our application. Besides, it opens/closes HTTP connection between UI and Java part. We can speed up the communication between browser part and Java UI using WebSocket protocol.

Add org.eclipse.jetty.websocket:websocket-server dependency to build.gradle:

```java
compile 'org.eclipse.jetty.websocket:websocket-server:9.3.20.v20170531'
```

Enable WebSocket for Vaadin application using @Push annotation on AppUI class:

```java
@Push(transport = Transport.WEBSOCKET)
@Theme(ValoTheme.THEME_NAME)
public class AppUI extends UI {

Remember to enable asynchronous support for AppServlet:

@WebServlet(urlPatterns = "/*", name = "AppServlet", asyncSupported = true)
```

Thanks to Vaadin, that is really easy!

### Unpack all the static files (CSS / images / fonts) and serve them directly from a file system instead of sending them via HTTP

Our application still sends all the static resources through Java servlets using network layer. We can make Electron read them from a file system directly!

As it is described here: https://github.com/electron/electron/blob/master/docs/api/protocol.md We can register custom protocol handler that will intercept requests to /VAADIN/ static files and read them from disk. Remember to unpack static files from jars on build stage!

See full example in:
- https://github.com/jreznot/electron-java-app/blob/master/build.gradle#L75
- https://github.com/jreznot/electron-java-app/blob/master/electron-src/main.js#L70

### Use Gradle Node.JS plugin com.moowork.node instead of manual Node installation

It is much easier to manage Node.js from build script than maintaining separate installation of it on developer machines. See example on: https://github.com/jreznot/electron-java-app/blob/master/build.gradle

## Real-life application

![Studio UI](/images/image4.png)

Take a look at the CUBA Studio - Development tool based on Java, Vaadin and Electron: https://www.cuba-platform.com/discuss/t/platform-cuba-studio-se-a-desktop-application-based-on-electron/2914
