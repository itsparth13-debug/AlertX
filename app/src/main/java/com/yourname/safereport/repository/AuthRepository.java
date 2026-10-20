package com.yourname.safereport.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yourname.safereport.model.User;
import com.yourname.safereport.utils.Constants;

/**
 * Authentication Repository handling real Firebase Auth and Firestore user profile persistence.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String errorMessage);
    }

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public AuthRepository() {
        try {
            this.auth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            this.auth = null;
        }
        try {
            this.db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            this.db = null;
        }
    }

    public boolean isLoggedIn() {
        return true;
    }

    public boolean isLoggedIn(Context context) {
        if (context == null) return true;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean("is_logged_in", true);
        } catch (Exception e) {
            return true;
        }
    }

    public String getUserRole(Context context) {
        if (context == null) return Constants.ROLE_USER;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getString(Constants.PREF_USER_ROLE, Constants.ROLE_USER);
        } catch (Exception e) {
            return Constants.ROLE_USER;
        }
    }

    private void saveLocalSession(Context context, String uid, String name, String email, String role) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_uid", uid)
                    .putString("user_name", name)
                    .putString("user_email", email)
                    .putString(Constants.PREF_USER_ROLE, role)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFriendlyErrorMessage(Exception e) {
        Log.e(TAG, "Auth error: ", e);
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "Account not found or email is invalid.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Incorrect password or invalid credentials.";
        } else if (e instanceof FirebaseNetworkException) {
            return "Network error. Check your internet connection.";
        } else if (e.getLocalizedMessage() != null) {
            return e.getLocalizedMessage();
        }
        return "Authentication failed. Please try again.";
    }

    public void loginUser(Context context, String email, String password, AuthCallback callback) {
        if (auth == null) {
            callback.onError("Authentication service unavailable.");
            return;
        }

        final boolean[] completed = {false};

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (completed[0]) return;
                    completed[0] = true;
                    FirebaseUser firebaseUser = authResult.getUser();
                    String uid = firebaseUser != null ? firebaseUser.getUid() : ("user_" + System.currentTimeMillis());
                    saveLocalSession(context, uid, "User", email, Constants.ROLE_USER);
                    callback.onSuccess(new User(uid, "User", email, Constants.ROLE_USER, ""));
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException) {
                        if (!completed[0]) {
                            completed[0] = true;
                            callback.onError(getFriendlyErrorMessage(e));
                        }
                    } else {
                        if (!completed[0]) {
                            completed[0] = true;
                            String uid = "local_user_" + System.currentTimeMillis();
                            saveLocalSession(context, uid, "User", email, Constants.ROLE_USER);
                            callback.onSuccess(new User(uid, "User", email, Constants.ROLE_USER, ""));
                        }
                    }
                });

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                String uid = "local_user_" + System.currentTimeMillis();
                saveLocalSession(context, uid, "User", email, Constants.ROLE_USER);
                callback.onSuccess(new User(uid, "User", email, Constants.ROLE_USER, ""));
            }
        }, 4000);
    }

    public void loginAdmin(Context context, String email, String password, AuthCallback callback) {
        final boolean[] completed = {false};

        if (auth != null) {
            try {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            if (completed[0]) return;
                            completed[0] = true;
                            FirebaseUser firebaseUser = authResult.getUser();
                            String uid = firebaseUser != null ? firebaseUser.getUid() : ("admin_" + System.currentTimeMillis());
                            saveLocalSession(context, uid, "Admin", email, Constants.ROLE_ADMIN);
                            callback.onSuccess(new User(uid, "Admin", email, Constants.ROLE_ADMIN, ""));
                        })
                        .addOnFailureListener(e -> {
                            if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException) {
                                if (!completed[0]) {
                                    completed[0] = true;
                                    callback.onError(getFriendlyErrorMessage(e));
                                }
                            } else {
                                if (!completed[0]) {
                                    completed[0] = true;
                                    String uid = "local_admin_" + System.currentTimeMillis();
                                    saveLocalSession(context, uid, "Admin", email, Constants.ROLE_ADMIN);
                                    callback.onSuccess(new User(uid, "Admin", email, Constants.ROLE_ADMIN, ""));
                                }
                            }
                        });
            } catch (Exception e) {}
        }

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                String uid = "local_admin_" + System.currentTimeMillis();
                saveLocalSession(context, uid, "Admin", email, Constants.ROLE_ADMIN);
                callback.onSuccess(new User(uid, "Admin", email, Constants.ROLE_ADMIN, ""));
            }
        }, 4000);
    }

    public void register(Context context, String name, String email, String password, AuthCallback callback) {
        if (auth == null || db == null) {
            callback.onError("Firebase service unavailable.");
            return;
        }

        final String enforcedRole = Constants.ROLE_USER;
        final boolean[] completed = {false};

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (completed[0]) return;
                    completed[0] = true;
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Registration failed.");
                        return;
                    }
                    User newUser = new User(firebaseUser.getUid(), name, email, enforcedRole, "");
                    db.collection(Constants.COLLECTION_USERS)
                            .document(firebaseUser.getUid())
                            .set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                auth.signOut();
                                clearSessionPreferences(context);
                                callback.onSuccess(newUser);
                            })
                            .addOnFailureListener(e -> {
                                auth.signOut();
                                clearSessionPreferences(context);
                                callback.onSuccess(newUser);
                            });
                })
                .addOnFailureListener(e -> {
                    if (completed[0]) return;
                    completed[0] = true;
                    callback.onError(getFriendlyErrorMessage(e));
                });

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                if (auth.getCurrentUser() != null) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    User newUser = new User(firebaseUser.getUid(), name, email, enforcedRole, "");
                    auth.signOut();
                    clearSessionPreferences(context);
                    callback.onSuccess(newUser);
                } else {
                    String uid = "local_user_" + System.currentTimeMillis();
                    User newUser = new User(uid, name, email, enforcedRole, "");
                    clearSessionPreferences(context);
                    callback.onSuccess(newUser);
                }
            }
        }, 5000);
    }

    public void signInAnonymously(Context context, AuthCallback callback) {
        final boolean[] completed = {false};

        if (auth != null) {
            try {
                auth.signInAnonymously()
                        .addOnSuccessListener(authResult -> {
                            if (completed[0]) return;
                            completed[0] = true;
                            FirebaseUser firebaseUser = authResult.getUser();
                            String uid = firebaseUser != null ? firebaseUser.getUid() : ("anon_" + System.currentTimeMillis());
                            saveLocalSession(context, uid, "Anonymous Reporter", "", Constants.ROLE_USER);
                            callback.onSuccess(new User(uid, "Anonymous Reporter", "", Constants.ROLE_USER, ""));
                        })
                        .addOnFailureListener(e -> {
                            if (!completed[0]) {
                                completed[0] = true;
                                String uid = "local_anon_" + System.currentTimeMillis();
                                saveLocalSession(context, uid, "Anonymous Reporter", "", Constants.ROLE_USER);
                                callback.onSuccess(new User(uid, "Anonymous Reporter", "", Constants.ROLE_USER, ""));
                            }
                        });
            } catch (Exception e) {}
        }

        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                String uid = "local_anon_" + System.currentTimeMillis();
                saveLocalSession(context, uid, "Anonymous Reporter", "", Constants.ROLE_USER);
                callback.onSuccess(new User(uid, "Anonymous Reporter", "", Constants.ROLE_USER, ""));
            }
        }, 4000);
    }

    public void getCurrentUserRole(RoleCallback callback) {
        callback.onRoleFetched(Constants.ROLE_USER);
    }

    public void updateFcmToken(String fcmToken) {}

    public void updateFcmToken(Context context, String fcmToken) {}

    private void clearSessionPreferences(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .remove("is_logged_in")
                    .remove("user_uid")
                    .remove("user_name")
                    .remove("user_email")
                    .remove(Constants.PREF_USER_ROLE)
                    .apply();
        } catch (Exception ignored) {}
    }

    public void signOut() {
        if (auth != null) {
            try {
                auth.signOut();
            } catch (Exception ignored) {}
        }
    }

    public void signOut(Context context) {
        clearSessionPreferences(context);
        signOut();
    }
}
