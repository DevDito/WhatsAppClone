package com.satriaamrudito.whatsappclone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.storage.FirebaseStorage
import com.satriaamrudito.whatsappclone.model.Constants.DATA_IMAGES
import com.satriaamrudito.whatsappclone.model.Constants.DATA_USERS
import com.satriaamrudito.whatsappclone.model.Constants.DATA_USER_EMAIL
import com.satriaamrudito.whatsappclone.model.Constants.DATA_USER_IMAGE_URL
import com.satriaamrudito.whatsappclone.model.Constants.DATA_USER_NAME
import com.satriaamrudito.whatsappclone.model.Constants.DATA_USER_PHONE
import com.satriaamrudito.whatsappclone.model.Constants.REQUEST_CODE_PHOTO
import com.satriaamrudito.whatsappclone.screen.LoginActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    private val firebaseDb = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (userId.isNullOrEmpty()) {
            finish()
        }

        progress_layout.setOnTouchListener { v, event -> true }
        btn_apply.setOnClickListener {
            onApply()
        }

        btn_delete_account.setOnClickListener {
            onDelete()
        }

        imbtn_profile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        }

        populateInfo()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            storeImage(data?.data)
        }
    }

    private fun storeImage(uri: Uri?) {
        if (uri != null) {
            Toast.makeText(this, "Upload....", Toast.LENGTH_SHORT).show()
            progress_layout.visibility = View.VISIBLE
            val filePath = firebaseStorage.child(DATA_IMAGES).child(userId!!)

            filePath.putFile(uri)
                .addOnSuccessListener {
                    filePath.downloadUrl
                        .addOnSuccessListener {

                            val url = it.toString()
                            Glide.with(this).load(uri).into(img_profile)
                            firebaseDb.collection(DATA_USERS)
                                .document(userId)
                                .update(DATA_USER_IMAGE_URL, url)
                                .addOnSuccessListener {
                                    imageUrl = url
                                    populateImage(this, imageUrl, img_profile, R.drawable.ic_user)
                                }
                            progress_layout.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            onUploadFailured()
                        }
                }
                .addOnFailureListener {
                    onUploadFailured()
                }
        }
    }

    private fun onUploadFailured() {
        Toast.makeText(this, "Image upload failed. Please try again later.", Toast.LENGTH_SHORT)
            .show()
        progress_layout.visibility = View.GONE
    }

    private fun populateImage(
        context: Context?,
        uri: String?,
        imageView: ImageView,
        errorDrawable: Int = R.drawable.empty
    ) {
        if (context != null) {
            val options =
                RequestOptions().placeholder(progressDrawable(context)).error(errorDrawable)
            Glide.with(context).load(uri).apply(options).into(imageView)
        }

    }

    private fun progressDrawable(context: Context): CircularProgressDrawable {
        return CircularProgressDrawable(context).apply {
            strokeWidth = 5f
            centerRadius = 30f
            start()
        }

    }

    private fun populateInfo() {
        progress_layout.visibility = View.VISIBLE
        firebaseDb.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                val user = it.toObject(com.satriaamrudito.whatsappclone.model.User::class.java)
                imageUrl = user?.imageUrl
                edt_name_profile.setText(user?.name, TextView.BufferType.EDITABLE)
                edt_email_profile.setText(user?.email, TextView.BufferType.EDITABLE)
                edt_phone_profile.setText(user?.phone, TextView.BufferType.EDITABLE)
                if (imageUrl != null) {
                    populateImage(this, user?.imageUrl, img_profile, R.drawable.ic_user)
                }
                progress_layout.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish()
            }
    }

    private fun onDelete() {
        progress_layout.visibility = View.VISIBLE
        AlertDialog.Builder(this)
            .setTitle("Delete Account") // Title AlertDialog
            .setMessage("This will delete your Profile Information. Are you sure?")
            .setPositiveButton("Yes") { dialog, which ->
                firebaseDb.collection(DATA_USERS).document(userId!!).delete()
                firebaseStorage.child(DATA_IMAGES).child(userId).delete()
                firebaseAuth.currentUser?.delete()
                    ?.addOnSuccessListener {
                        finish()
                    }
                    ?.addOnFailureListener {
                        finish()
                    }
                progress_layout.visibility = View.GONE
                Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { dialog, which ->
                progress_layout.visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }

    private fun onApply() {
        progress_layout.visibility = View.VISIBLE
        val name = edt_name_profile.text.toString()
        val email = edt_email_profile.text.toString()
        val phone = edt_phone_profile.text.toString()
        val map = HashMap<String, Any>()
        map[DATA_USER_NAME] = name
        map[DATA_USER_EMAIL] = email
        map[DATA_USER_PHONE] = phone

        firebaseDb.collection(DATA_USERS).document(userId!!).update(map)
            .addOnSuccessListener {
                Toast.makeText(this, "Update Succses", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
                progress_layout.visibility = View.GONE
            }
    }

}