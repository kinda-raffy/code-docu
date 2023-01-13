import com.intellij.lang.Commenter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import xyz.kindaraffy.codedocu.ApiService


/* Editor context menu. */
class CodeDocuAction : AnAction() {
    // Only show dialog when user places cursor on comment.
    override fun update(e: AnActionEvent) {
        val psiFile = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if ((psiFile == null  ||  editor == null)) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        val isComment: Boolean = element!!.node.elementType.toString().contains("COMMENT")
        val isDocComment: Boolean = element.node.elementType.toString().contains("DOC")
        println(element!!.node.elementType.toString())
        e.presentation.isEnabledAndVisible = isComment || isDocComment
    }

    fun containsStartTag(element: PsiElement): Boolean {
        return element.node.elementType.toString().contains("START")
    }

    fun containsEndTag(element: PsiElement): Boolean {
        return element.node.elementType.toString().contains("END")
    }

    fun getDocComment(caretElement: PsiElement, editor: Editor): String {
        val document: Document = editor.document
        var docComment: String = document.getText(
            TextRange(caretElement.textRange.startOffset, caretElement.textRange.endOffset)
        )
        // Traverse elements form the caret element to gather all doc comments.
        var iterElement: PsiElement = caretElement
        // Backwards traversal.
        while (!containsStartTag(iterElement)) {
            iterElement = iterElement.prevSibling
            docComment = document.getText(
                TextRange(iterElement.textRange.startOffset, iterElement.textRange.endOffset)
            ) + docComment
        }
        // Forwards traversal.
        iterElement = caretElement
        while (!containsEndTag(iterElement)) {
            iterElement = iterElement.nextSibling
            docComment += document.getText(
                TextRange(iterElement.textRange.startOffset, iterElement.textRange.endOffset)
            )
        }
        return docComment
    }

    fun getEntireComment(file: PsiFile, editor: Editor): String {
        val caretModel: CaretModel = editor.caretModel
        val caretPosition: Int = caretModel.primaryCaret.offset
        val element: PsiElement? = file.findElementAt(caretPosition)

        val elementType: String = element!!.node.elementType.toString()
        val isComment: Boolean = elementType.contains("COMMENT")
            .and(!elementType.contains("DOC"))
        val isDocComment: Boolean = elementType.contains("DOC")

        if (isComment) {
            println("isComment")
//            val language: Language = element.language
//            val commenter: Commenter = getCommenter(psiFile, editor, language, language)!!
//            val comment: String = clean(uncomment(element, commenter))
            return element.text
        }
        if (isDocComment) {
            println("isDocComment")
            return getDocComment(element, editor)
        }
        return "No comment found."
    }

    fun getSelectionComment(selectionModel: SelectionModel): String {
        // Get the doc string or code doc for the selected text
        val document: Document = selectionModel.editor.document
        return document.getText(
            TextRange(selectionModel.selectionStart, selectionModel.selectionEnd)
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val file: PsiFile = e.getRequiredData(LangDataKeys.PSI_FILE)
        val editor: Editor = e.getRequiredData(PlatformDataKeys.EDITOR)

        // Get the current selection in the editor
        val selectionModel: SelectionModel = editor.selectionModel
        val hasSelection: Boolean = selectionModel.hasSelection()
        val comment: String = if (hasSelection)
            getSelectionComment(selectionModel) else getEntireComment(file, editor)

        // Display the doc string or code doc in a dialog box
        Messages.showInfoMessage(comment, "Detected Comment")

        val modifiedComment: String = ApiService(comment).getCompletion()

        placeCompletionComment(modifiedComment, editor, file, selectionModel, hasSelection)
    }

    fun placeCompletionComment(
        comment: String,
        editor: Editor,
        file: PsiFile,
        selectionModel: SelectionModel,
        hasSelection: Boolean
    ) {
        var start: Int = selectionModel.selectionStart
        var end: Int = selectionModel.selectionEnd
        if (!hasSelection) {
            // TODO: Bubble this to top and inject into get entire comment.
            val caretModel: CaretModel = editor.caretModel
            val caretPosition: Int = caretModel.primaryCaret.offset
            val element: PsiElement = file.findElementAt(caretPosition)!!

            start = element.textRange.startOffset
            end = element.textRange.endOffset
        }
        replaceText(editor, start, end, comment)
    }

    fun replaceText(editor: Editor, start: Int, end: Int, text: String) {
        WriteCommandAction.runWriteCommandAction(editor.project) {
            editor.document.replaceString(start, end, text)
        }
    }

    /* Helpers */
    fun uncomment(comment: PsiComment, commenter: Commenter) : String {
        val text: String = comment.text.trim()
        // Block Comment.
        val blockPrefix: String = commenter.blockCommentPrefix!!
        val blockSuffix: String = commenter.blockCommentSuffix!!
        if (text.startsWith(blockPrefix)) {
            return text.removePrefix(blockPrefix).removeSuffix(blockSuffix)
        }
        // Line Comment.
        return text.removePrefix(commenter.lineCommentPrefix!!)
    }

    fun clean(comment: String) : String { return comment.trim() }
}
