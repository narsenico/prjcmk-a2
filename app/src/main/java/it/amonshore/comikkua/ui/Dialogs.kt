package it.amonshore.comikkua.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.DialogConfirmBinding

context (Fragment)
fun showCancellableDialog(
    title: String,
    message: String,
    onCancel: () -> Unit
): Dialog = AlertDialog.Builder(activity)
    .setTitle(title)
    .setMessage(message)
    .setCancelable(false)
    .setNegativeButton(android.R.string.cancel) { _, _ ->
        onCancel()
    }
    .create()
    .also {
        it.show()
    }

context (Fragment)
fun showErrorDialog(
    title: String,
    message: String
): Dialog = AlertDialog.Builder(activity)
    .setTitle(title)
    .setMessage(message)
    .setIcon(R.drawable.ic_warning)
    .setPositiveButton(android.R.string.ok, null)
    .create()
    .also {
        it.show()
    }

context (Fragment)
fun showConfirmDialog(
    title: String,
    message: String,
    confirmPhrase: String,
    onAccept: () -> Unit,
): Dialog {
    val binding = DialogConfirmBinding.inflate(layoutInflater)
    binding.message.text = message

    val dialog = AlertDialog.Builder(activity)
        .setTitle(title)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onAccept()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    binding.txtConfirm.doAfterTextChanged { text ->
        text?.run {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled =
                toString() == confirmPhrase
        }
    }

    dialog.setOnShowListener {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
    }

    dialog.show()
    return dialog
}