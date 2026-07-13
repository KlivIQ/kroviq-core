package kroviq.wrapper.factory;

import kroviq.wrapper.core.RichTextHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class GenericRichTextHandler implements RichTextHandler {

    private static final Logger logger = LogManager.getLogger(GenericRichTextHandler.class);

    @Override
    public void setContent(WebDriver driver, WebElement editorElement, String htmlContent) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Try editor-specific API first
        if (setViaEditorApi(js, editorElement, htmlContent)) {
            logger.info("[RichText] Content set via editor API");
            return;
        }

        // Fallback: direct innerHTML on contenteditable
        js.executeScript(
                "arguments[0].innerHTML = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                editableArea, htmlContent);
        logger.info("[RichText] Content set via innerHTML");
    }

    @Override
    public void setPlainText(WebDriver driver, WebElement editorElement, String text) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Try editor-specific text API
        if (setTextViaApi(js, editorElement, text)) {
            logger.info("[RichText] Plain text set via editor API");
            return;
        }

        // Fallback: innerText
        js.executeScript(
                "arguments[0].innerText = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                editableArea, text);
        logger.info("[RichText] Plain text set via innerText");
    }

    @Override
    public void appendContent(WebDriver driver, WebElement editorElement, String htmlContent) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "arguments[0].innerHTML += arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                editableArea, htmlContent);
        logger.info("[RichText] Content appended");
    }

    @Override
    public void clearContent(WebDriver driver, WebElement editorElement) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "arguments[0].innerHTML = '';" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                editableArea);
        logger.info("[RichText] Content cleared");
    }

    @Override
    public String getContent(WebDriver driver, WebElement editorElement) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object result = js.executeScript("return arguments[0].innerHTML;", editableArea);
        return result != null ? result.toString() : "";
    }

    @Override
    public String getPlainText(WebDriver driver, WebElement editorElement) {
        WebElement editableArea = resolveEditableArea(driver, editorElement);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object result = js.executeScript("return arguments[0].innerText || arguments[0].textContent;", editableArea);
        return result != null ? result.toString().trim() : "";
    }

    private WebElement resolveEditableArea(WebDriver driver, WebElement editorElement) {
        // Check if element itself is contenteditable
        String contentEditable = editorElement.getAttribute("contenteditable");
        if ("true".equals(contentEditable)) return editorElement;

        // Quill: .ql-editor
        List<WebElement> quill = editorElement.findElements(By.cssSelector(".ql-editor[contenteditable='true']"));
        if (!quill.isEmpty()) return quill.get(0);

        // CKEditor 5: .ck-editor__editable
        List<WebElement> ck5 = editorElement.findElements(By.cssSelector(".ck-editor__editable[contenteditable='true']"));
        if (!ck5.isEmpty()) return ck5.get(0);

        // ProseMirror: .ProseMirror
        List<WebElement> pm = editorElement.findElements(By.cssSelector(".ProseMirror[contenteditable='true']"));
        if (!pm.isEmpty()) return pm.get(0);

        // Draft.js: .DraftEditor-editorContainer [contenteditable]
        List<WebElement> draft = editorElement.findElements(By.cssSelector("[contenteditable='true']"));
        if (!draft.isEmpty()) return draft.get(0);

        // TinyMCE iframe: find iframe and switch
        List<WebElement> iframes = editorElement.findElements(By.cssSelector("iframe"));
        if (!iframes.isEmpty()) {
            driver.switchTo().frame(iframes.get(0));
            WebElement body = driver.findElement(By.cssSelector("body[contenteditable='true'], body"));
            return body;
        }

        // Generic: any contenteditable descendant
        List<WebElement> any = editorElement.findElements(By.cssSelector("[contenteditable='true']"));
        if (!any.isEmpty()) return any.get(0);

        // Last resort: the element itself
        return editorElement;
    }

    private boolean setViaEditorApi(JavascriptExecutor js, WebElement element, String content) {
        try {
            // Quill
            Object result = js.executeScript(
                    "var el = arguments[0].querySelector('.ql-editor') || arguments[0];" +
                    "if (el.__quill) { el.__quill.root.innerHTML = arguments[1]; return true; }" +
                    "var quillEl = arguments[0].closest('.quill') || arguments[0];" +
                    "if (quillEl && quillEl.__quill) { quillEl.__quill.root.innerHTML = arguments[1]; return true; }" +
                    "return false;", element, content);
            if (Boolean.TRUE.equals(result)) return true;
        } catch (Exception e) { /* not Quill */ }

        try {
            // TinyMCE
            Object result = js.executeScript(
                    "if (typeof tinymce !== 'undefined' && tinymce.activeEditor) {" +
                    "  tinymce.activeEditor.setContent(arguments[1]); return true;" +
                    "} return false;", element, content);
            if (Boolean.TRUE.equals(result)) return true;
        } catch (Exception e) { /* not TinyMCE */ }

        try {
            // CKEditor 5
            Object result = js.executeScript(
                    "var ck = arguments[0].querySelector('.ck-editor__editable');" +
                    "if (ck && ck.ckeditorInstance) { ck.ckeditorInstance.setData(arguments[1]); return true; }" +
                    "return false;", element, content);
            if (Boolean.TRUE.equals(result)) return true;
        } catch (Exception e) { /* not CKEditor 5 */ }

        try {
            // CKEditor 4
            Object result = js.executeScript(
                    "if (typeof CKEDITOR !== 'undefined') {" +
                    "  var names = Object.keys(CKEDITOR.instances);" +
                    "  if (names.length > 0) { CKEDITOR.instances[names[0]].setData(arguments[1]); return true; }" +
                    "} return false;", element, content);
            if (Boolean.TRUE.equals(result)) return true;
        } catch (Exception e) { /* not CKEditor 4 */ }

        return false;
    }

    private boolean setTextViaApi(JavascriptExecutor js, WebElement element, String text) {
        try {
            Object result = js.executeScript(
                    "var el = arguments[0].querySelector('.ql-editor') || arguments[0];" +
                    "if (el.__quill) { el.__quill.setText(arguments[1]); return true; }" +
                    "return false;", element, text);
            if (Boolean.TRUE.equals(result)) return true;
        } catch (Exception e) { /* not Quill */ }
        return false;
    }
}
