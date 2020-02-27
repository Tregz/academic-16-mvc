package com.tregz.miksing.data.user;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tregz.miksing.arch.auth.AuthUtil;
import com.tregz.miksing.data.DataNotation;
import com.tregz.miksing.data.DataReference;
import com.tregz.miksing.data.DataUpdate;
import com.tregz.miksing.data.user.tube.UserTubeListener;

import java.util.Date;

import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UserListener implements MaybeObserver<User>, ValueEventListener {
    private final String TAG = UserListener.class.getSimpleName();

    private Context context;
    private DatabaseReference table;
    private User user;
    private String userId;
    private UserAccess access;

    public UserListener(Context context, String userId) {
        this.context = context;
        this.userId = userId;
        table().child(userId).child("data").addListenerForSingleValueEvent(this);
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        Log.e(TAG, databaseError.getMessage());
    }

    @Override
    public void onComplete() {
        if (user != null) new UserInsert(context, user);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            // Create initial user data
            user = new User(userId, new Date());
            FirebaseUser firebaseUser =  AuthUtil.user();
            user.setName(firebaseUser.getDisplayName());
            user.setEmail(firebaseUser.getEmail());
            table().child(userId).child("data").setValue(UserUtil.map(user));
        } else {
            Long createdAt = snapshot.child(DataNotation.CD).getValue(Long.class);
            if (createdAt != null) {
                user = new User(userId, new Date(createdAt));
                user.setName(snapshot.child(DataNotation.NS).getValue(String.class));
                user.setEmail(snapshot.child(DataNotation.AS).getValue(String.class));
            }
        }
        access().whereId(userId).subscribeOn(Schedulers.io()).subscribe(this);
    }

    @Override
    public void onError(Throwable e) {
        Log.d(TAG, "onError? " + e.getMessage());
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(User t) {
        if (user != null) {
            new DataUpdate(access().update(user));
            new UserTubeListener(context, userId);
        }
    }

    private DatabaseReference table() {
        if (table == null) table = FirebaseDatabase.getInstance().getReference(User.TABLE);
        return table;
    }

    private UserAccess access() {
        if (access == null) access = DataReference.getInstance(context).accessUser();
        return access;
    }
}
