package com.example.goldgallery;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PrivateFragment extends Fragment {

    private PhotoAdapter privateAdapter;
    private boolean isUnlocked = false;

    public PrivateFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_private, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rvPrivatePhotos);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        privateAdapter = new PhotoAdapter(
                PrivatePhotosStore.INSTANCE.getAll(),
                photoUri -> {
                    android.content.Intent intent = new android.content.Intent(requireContext(), FullImageActivity.class);
                    intent.putExtra("IMAGE_PATH", photoUri);
                    startActivity(intent);
                    return kotlin.Unit.INSTANCE;
                },
                (photoUri, anchor) -> {
                    showPrivatePhotoActions(photoUri);
                    return kotlin.Unit.INSTANCE;
                }
        );
        privateAdapter.setBlurred(true);

        recyclerView.setAdapter(privateAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isUnlocked) {
            if (privateAdapter != null) {
                privateAdapter.setBlurred(true);
            }
            showAuthFlow();
            return;
        }
        if (privateAdapter != null) {
            privateAdapter.updatePhotos(PrivatePhotosStore.INSTANCE.getAll());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isUnlocked = false;
        if (privateAdapter != null) {
            privateAdapter.setBlurred(true);
        }
    }

    private void showAuthFlow() {
        if (!isAdded()) {
            return;
        }
        if (!PrivateAccessStore.INSTANCE.hasPin(requireContext())) {
            showCreatePinDialog();
        } else {
            showUnlockDialog();
        }
    }

    private void showCreatePinDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        EditText pinInput = createPinInput();
        EditText questionInput = createTextInput();
        questionInput.setHint(getString(R.string.private_security_question_hint));
        EditText answerInput = createTextInput();
        answerInput.setHint(getString(R.string.private_security_answer_hint));
        TextView errorText = createErrorText();

        LinearLayout layout = buildDialogLayout(pinInput, questionInput, answerInput, errorText);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.private_create_pin_title)
                .setMessage(R.string.private_create_pin_message)
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_pin, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String pin = pinInput.getText().toString().trim();
                    String question = questionInput.getText().toString().trim();
                    String answer = answerInput.getText().toString().trim();

                    if (!isValidPin(pin)) {
                        errorText.setText(getString(R.string.private_invalid_pin));
                        errorText.setVisibility(View.VISIBLE);
                        pinInput.requestFocus();
                        return;
                    }
                    if (question.isEmpty() || answer.isEmpty()) {
                        errorText.setText(getString(R.string.private_recovery_required));
                        errorText.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (!isAdded()) return;
                    PrivateAccessStore.INSTANCE.savePinAndRecovery(context, pin, question, answer);
                    dialog.dismiss();
                    unlockPrivateArea();
                }));

        dialog.show();
    }

    private void showUnlockDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_private_pin_unlock, null, false);
        TextView errorText = dialogView.findViewById(R.id.tvPinError);
        TextView forgotPin = dialogView.findViewById(R.id.tvForgotPin);
        AppCompatButton cancelButton = dialogView.findViewById(R.id.btnPinCancel);
        AppCompatButton verifyButton = dialogView.findViewById(R.id.btnPinVerify);

        EditText[] pinDigits = new EditText[]{
                dialogView.findViewById(R.id.etPinDigit1),
                dialogView.findViewById(R.id.etPinDigit2),
                dialogView.findViewById(R.id.etPinDigit3),
                dialogView.findViewById(R.id.etPinDigit4)
        };
        String[] actualPinDigits = new String[pinDigits.length];
        setupPinDigitInputs(pinDigits, actualPinDigits);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        forgotPin.setOnClickListener(v -> {
            dialog.dismiss();
            showSecurityQuestionDialog();
        });
        verifyButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            String pin = collectPin(actualPinDigits);
            if (!isValidPin(pin)) {
                errorText.setText(getString(R.string.private_invalid_pin));
                errorText.setVisibility(View.VISIBLE);
                clearPinDigits(pinDigits, actualPinDigits);
                pinDigits[0].requestFocus();
                return;
            }
            if (PrivateAccessStore.INSTANCE.verifyPin(context, pin)) {
                dialog.dismiss();
                unlockPrivateArea();
            } else {
                errorText.setText(getString(R.string.private_wrong_pin));
                errorText.setVisibility(View.VISIBLE);
                clearPinDigits(pinDigits, actualPinDigits);
                pinDigits[0].requestFocus();
            }
        });

        dialog.show();
        pinDigits[0].requestFocus();
    }

    private void showSecurityQuestionDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        String question = PrivateAccessStore.INSTANCE.getSecurityQuestion(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_private_forgot_pin, null, false);
        TextView questionText = dialogView.findViewById(R.id.tvRecoveryQuestion);
        EditText answerInput = dialogView.findViewById(R.id.etRecoveryAnswer);
        TextView errorText = dialogView.findViewById(R.id.tvRecoveryError);
        AppCompatButton cancelButton = dialogView.findViewById(R.id.btnRecoveryCancel);
        AppCompatButton verifyButton = dialogView.findViewById(R.id.btnRecoveryVerify);

        questionText.setText(question);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            showUnlockDialog();
        });
        verifyButton.setOnClickListener(v -> {
                    if (!isAdded()) return;
                    String answer = answerInput.getText().toString();
                    if (PrivateAccessStore.INSTANCE.verifySecurityAnswer(context, answer)) {
                        dialog.dismiss();
                        showResetPinDialog();
                    } else {
                        errorText.setText(getString(R.string.private_wrong_answer));
                        errorText.setVisibility(View.VISIBLE);
                        answerInput.requestFocus();
                    }
                });

        dialog.show();
    }

    private void showResetPinDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_private_pin_unlock, null, false);
        TextView titleText = dialogView.findViewById(R.id.tvPinModalTitle);
        TextView subtitleText = dialogView.findViewById(R.id.tvPinModalSubtitle);
        TextView errorText = dialogView.findViewById(R.id.tvPinError);
        TextView forgotPin = dialogView.findViewById(R.id.tvForgotPin);
        AppCompatButton cancelButton = dialogView.findViewById(R.id.btnPinCancel);
        AppCompatButton verifyButton = dialogView.findViewById(R.id.btnPinVerify);
        EditText[] pinDigits = new EditText[]{
                dialogView.findViewById(R.id.etPinDigit1),
                dialogView.findViewById(R.id.etPinDigit2),
                dialogView.findViewById(R.id.etPinDigit3),
                dialogView.findViewById(R.id.etPinDigit4)
        };
        String[] actualPinDigits = new String[pinDigits.length];

        titleText.setText(R.string.private_reset_pin_title);
        subtitleText.setText(R.string.private_reset_pin_modal_subtitle);
        forgotPin.setVisibility(View.GONE);
        verifyButton.setText(R.string.reset_pin);
        setupPinDigitInputs(pinDigits, actualPinDigits);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        verifyButton.setOnClickListener(v -> {
                    String newPin = collectPin(actualPinDigits);
                    if (!isValidPin(newPin)) {
                        errorText.setText(getString(R.string.private_invalid_pin));
                        errorText.setVisibility(View.VISIBLE);
                        clearPinDigits(pinDigits, actualPinDigits);
                        pinDigits[0].requestFocus();
                        return;
                    }
                    if (!isAdded()) return;
                    PrivateAccessStore.INSTANCE.resetPin(context, newPin);
                    dialog.dismiss();
                    Toast.makeText(context, getString(R.string.private_pin_reset_success), Toast.LENGTH_SHORT).show();
                    showUnlockDialog();
                });

        dialog.show();
        pinDigits[0].requestFocus();
    }

    private void unlockPrivateArea() {
        isUnlocked = true;
        if (privateAdapter != null) {
            privateAdapter.updatePhotos(PrivatePhotosStore.INSTANCE.getAll());
            privateAdapter.setBlurred(false);
        }

        refreshEmptyState();
    }

    private void refreshEmptyState() {
        View root = getView();
        if (root == null) return;
        TextView emptyText = root.findViewById(R.id.tvPrivateEmpty);
        if (emptyText == null) return;
        emptyText.setVisibility(PrivatePhotosStore.INSTANCE.getAll().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showPrivatePhotoActions(String photoUri) {
        String[] options = new String[]{getString(R.string.remove_from_private)};

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.private_photo_actions_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0 && PrivatePhotosStore.INSTANCE.remove(photoUri)) {
                        privateAdapter.removePhoto(photoUri);
                        refreshEmptyState();
                        Toast.makeText(requireContext(), getString(R.string.removed_from_private_message), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private EditText createPinInput() {
        EditText input = createTextInput();
        input.setHint(getString(R.string.private_pin_hint));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        return input;
    }

    private EditText createTextInput() {
        EditText input = new EditText(requireContext());
        int horizontalPadding = dpToPx(14);
        int verticalPadding = dpToPx(10);
        input.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setBackgroundResource(android.R.drawable.edit_text);
        return input;
    }

    private TextView createErrorText() {
        TextView errorText = new TextView(requireContext());
        errorText.setTextColor(0xFFB00020);
        errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        errorText.setVisibility(View.GONE);
        return errorText;
    }

    private LinearLayout buildDialogLayout(View... views) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        layout.setPadding(padding, dpToPx(8), padding, 0);
        for (View child : views) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dpToPx(10);
            layout.addView(child, params);
        }
        return layout;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private void setupPinDigitInputs(EditText[] pinDigits, String[] actualPinDigits) {
        for (int i = 0; i < pinDigits.length; i++) {
            int index = i;
            EditText current = pinDigits[i];
            EditText next = i < pinDigits.length - 1 ? pinDigits[i + 1] : null;
            EditText previous = i > 0 ? pinDigits[i - 1] : null;

            current.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            current.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            current.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
            current.setTransformationMethod(PasswordTransformationMethod.getInstance());
            current.setLongClickable(false);
            current.setTextIsSelectable(false);
            current.setHint("•");

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 0) {
                        actualPinDigits[index] = "";
                        return;
                    }

                    char typedChar = s.charAt(s.length() - 1);
                    if (!Character.isDigit(typedChar)) {
                        return;
                    }

                    actualPinDigits[index] = String.valueOf(typedChar);
                    if (!"•".contentEquals(s)) {
                        current.removeTextChangedListener(this);
                        current.setText("•");
                        current.setSelection(current.getText().length());
                        current.addTextChangedListener(this);
                    }

                    if (next != null) {
                        next.requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            };
            current.addTextChangedListener(watcher);

            current.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && current.getText().toString().isEmpty() && previous != null) {
                    previous.requestFocus();
                    previous.setSelection(previous.getText().length());
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && !current.getText().toString().isEmpty()) {
                    actualPinDigits[index] = "";
                    current.setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private String collectPin(String[] actualPinDigits) {
        StringBuilder builder = new StringBuilder();
        for (String pinDigit : actualPinDigits) {
            if (pinDigit != null) {
                builder.append(pinDigit.trim());
            }
        }
        return builder.toString();
    }

    private void clearPinDigits(EditText[] pinDigits, String[] actualPinDigits) {
        for (int i = 0; i < pinDigits.length; i++) {
            pinDigits[i].setText("");
            actualPinDigits[i] = "";
        }
    }

    private boolean isValidPin(String pin) {
        return pin.matches("^\\d{4}$");
    }
}
