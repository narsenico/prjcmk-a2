package it.amonshore.comikkua.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.core.widget.doAfterTextChanged
import it.amonshore.comikkua.R
import it.amonshore.comikkua.databinding.DialogConfirmBinding

fun showCancellableDialog(
    activity: Activity,
    title: String,
    message: String,
    onCancel: () -> Unit
): Dialog = AlertDialog.Builder(activity)
    .setTitle(title)
    .setMessage(message)
    .setCancelable(false)
    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
        dialog.dismiss()
        onCancel()
    }
    .create()
    .also {
        it.show()
    }

fun showErrorDialog(
    activity: Activity,
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

fun showConfirmDialog(
    activity: Activity,
    title: String,
    message: String,
    confirmPhrase: String,
    onAccept: () -> Unit,
): Dialog {
    val inflater = activity.layoutInflater;
    val binding = DialogConfirmBinding.inflate(inflater)
    binding.message.text = message

    val dialog = AlertDialog.Builder(activity)
        .setTitle(title)
        .setView(binding.root)
        .setCancelable(false)
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