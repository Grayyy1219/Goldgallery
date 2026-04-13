package com.example.goldgallery;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
        EditText pinInput = createPinInput();
        EditText questionInput = new EditText(requireContext());
        questionInput.setHint(getString(R.string.private_security_question_hint));
        EditText answerInput = new EditText(requireContext());
        answerInput.setHint(getString(R.string.private_security_answer_hint));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);
        layout.addView(pinInput);
        layout.addView(questionInput);
        layout.addView(answerInput);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.private_create_pin_title)
                .setMessage(R.string.private_create_pin_message)
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.save_pin, (dialog, which) -> {
                    String pin = pinInput.getText().toString().trim();
                    String question = questionInput.getText().toString().trim();
                    String answer = answerInput.getText().toString().trim();

                    if (!isValidPin(pin)) {
                        Toast.makeText(requireContext(), getString(R.string.private_invalid_pin), Toast.LENGTH_SHORT).show();
                        showCreatePinDialog();
                        return;
                    }
                    if (question.isEmpty() || answer.isEmpty()) {
                        Toast.makeText(requireContext(), getString(R.string.private_recovery_required), Toast.LENGTH_SHORT).show();
                        showCreatePinDialog();
                        return;
                    }

                    PrivateAccessStore.INSTANCE.savePinAndRecovery(requireContext(), pin, question, answer);
                    unlockPrivateArea();
                })
                .show();
    }

    private void showUnlockDialog() {
        EditText pinInput = createPinInput();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.private_unlock_title)
                .setMessage(R.string.private_unlock_message)
                .setView(pinInput)
                .setCancelable(true)
                .setPositiveButton(R.string.unlock, (dialog, which) -> {
                    String pin = pinInput.getText().toString().trim();
                    if (PrivateAccessStore.INSTANCE.verifyPin(requireContext(), pin)) {
                        unlockPrivateArea();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.private_wrong_pin), Toast.LENGTH_SHORT).show();
                        showUnlockDialog();
                    }
                })
                .setNeutralButton(R.string.private_forgot_pin, (dialog, which) -> showSecurityQuestionDialog())
                .show();
    }

    private void showSecurityQuestionDialog() {
        EditText answerInput = new EditText(requireContext());
        answerInput.setHint(getString(R.string.private_security_answer_hint));

        String question = PrivateAccessStore.INSTANCE.getSecurityQuestion(requireContext());
        String message = getString(R.string.private_answer_question_prefix) + "\n" + question;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.private_forgot_pin_title)
                .setMessage(message)
                .setView(answerInput)
                .setPositiveButton(R.string.verify, (dialog, which) -> {
                    String answer = answerInput.getText().toString();
                    if (PrivateAccessStore.INSTANCE.verifySecurityAnswer(requireContext(), answer)) {
                        showResetPinDialog();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.private_wrong_answer), Toast.LENGTH_SHORT).show();
                        showUnlockDialog();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> showUnlockDialog())
                .show();
    }

    private void showResetPinDialog() {
        EditText pinInput = createPinInput();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.private_reset_pin_title)
                .setMessage(R.string.private_reset_pin_message)
                .setView(pinInput)
                .setCancelable(false)
                .setPositiveButton(R.string.reset_pin, (dialog, which) -> {
                    String newPin = pinInput.getText().toString().trim();
                    if (!isValidPin(newPin)) {
                        Toast.makeText(requireContext(), getString(R.string.private_invalid_pin), Toast.LENGTH_SHORT).show();
                        showResetPinDialog();
                        return;
                    }
                    PrivateAccessStore.INSTANCE.resetPin(requireContext(), newPin);
                    Toast.makeText(requireContext(), getString(R.string.private_pin_reset_success), Toast.LENGTH_SHORT).show();
                    showUnlockDialog();
                })
                .show();
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
        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.private_pin_hint));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        return input;
    }

    private boolean isValidPin(String pin) {
        return pin.matches("^\\d{4,6}$");
    }
}
