package com.yourname.safereport.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yourname.safereport.model.User;
import com.yourname.safereport.repository.AuthRepository;

/**
 * ViewModel managing user authentication and user profile state.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    private final MutableLiveData<User> userState = new MutableLiveData<>();
    private final MutableLiveData<String> errorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingState = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AuthRepository();
    }

    public LiveData<User> getUserState() {
        return userState;
    }

    public LiveData<String> getErrorState() {
        return errorState;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public boolean isLoggedIn() {
        return repository.isLoggedIn(getApplication());
    }

    public String getUserRole() {
        return repository.getUserRole(getApplication());
    }

    public void loginUser(String email, String password) {
        if (email.trim().isEmpty() || password.trim().isEmpty()) {
            loadingState.setValue(false);
            errorState.setValue("Email and password cannot be empty.");
            return;
        }

        loadingState.setValue(true);
        repository.loginUser(getApplication(), email.trim(), password.trim(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                loadingState.setValue(false);
                userState.setValue(user);
            }

            @Override
            public void onError(String errorMessage) {
                loadingState.setValue(false);
                errorState.setValue(errorMessage);
            }
        });
    }

    public void loginAdmin(String email, String password) {
        if (email.trim().isEmpty() || password.trim().isEmpty()) {
            loadingState.setValue(false);
            errorState.setValue("Email and password cannot be empty.");
            return;
        }

        loadingState.setValue(true);
        repository.loginAdmin(getApplication(), email.trim(), password.trim(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                loadingState.setValue(false);
                userState.setValue(user);
            }

            @Override
            public void onError(String errorMessage) {
                loadingState.setValue(false);
                errorState.setValue(errorMessage);
            }
        });
    }

    public void register(String name, String email, String password) {
        if (name.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            loadingState.setValue(false);
            errorState.setValue("All fields are required.");
            return;
        }

        if (password.trim().length() < 6) {
            loadingState.setValue(false);
            errorState.setValue("Password must be at least 6 characters long.");
            return;
        }

        loadingState.setValue(true);
        repository.register(getApplication(), name.trim(), email.trim(), password.trim(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                loadingState.setValue(false);
                userState.setValue(user);
            }

            @Override
            public void onError(String errorMessage) {
                loadingState.setValue(false);
                errorState.setValue(errorMessage);
            }
        });
    }

    public void register(String name, String email, String password, String role) {
        register(name, email, password);
    }

    public void signInAnonymously() {
        loadingState.setValue(true);
        repository.signInAnonymously(getApplication(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                loadingState.setValue(false);
                userState.setValue(user);
            }

            @Override
            public void onError(String errorMessage) {
                loadingState.setValue(false);
                errorState.setValue(errorMessage);
            }
        });
    }

    public void signOut() {
        repository.signOut(getApplication());
    }
}
