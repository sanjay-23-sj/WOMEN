package com.sanx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sanx.app.data.local.entity.Contact
import com.sanx.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.sanx.app.ui.viewmodel.MainViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.ContactsContract
import androidx.compose.ui.platform.LocalContext

/**
 * Trusted Circle Screen.
 * Add, edit, and manage emergency contacts with permission configurations.
 */
@Composable
fun ContactsScreen(viewModel: MainViewModel, onMenuClick: () -> Unit) {
    val contacts by viewModel.contacts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(SanXBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Safety Contact",
                    subtitle = "Your primary emergency contact",
                    onMenuClick = onMenuClick
                )
            }

            item {
                InfoBox(
                    icon = Icons.Default.Security,
                    text = "Your safety contact will receive your real-time location, battery level, and emergency severity during active alerts.",
                    color = SanXInfo
                )
            }

            items(contacts) { contact ->
                ContactCard(
                    contact = contact,
                    onEdit = { editingContact = contact },
                    onDelete = { viewModel.removeContact(contact) }
                )
            }

            if (contacts.isEmpty()) {
                item {
                    EmptyContactsHint()
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // FAB to add contact (only visible if no contact has been configured yet)
        if (contacts.isEmpty()) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = SanXSafe,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        ContactDialog(
            contact = null,
            onDismiss = { showAddDialog = false },
            onSave = { contact ->
                viewModel.addContact(contact)
                showAddDialog = false
            }
        )
    }

    editingContact?.let { existing ->
        ContactDialog(
            contact = existing,
            onDismiss = { editingContact = null },
            onSave = { updated ->
                viewModel.updateContact(updated)
                editingContact = null
            }
        )
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = SanXEmergency
    val priorityLabel = "Primary Contact"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SanXCard)
            .border(1.dp, SanXBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar initials
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(priorityColor.copy(alpha = 0.15f))
                .border(1.5.dp, priorityColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.take(2).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = priorityColor
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(contact.name, style = MaterialTheme.typography.titleMedium)
            Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTag(priorityLabel, priorityColor)
                if (contact.notifyViaSms) MiniTag("SMS", SanXInfo)
                if (contact.shareLocation) MiniTag("GPS", SanXSafe)
            }
        }

        // Actions
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit",
                    tint = SanXTextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete",
                    tint = SanXEmergency.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MiniTag(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = color, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun EmptyContactsHint() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null,
            tint = SanXTextDisabled, modifier = Modifier.size(48.dp))
        Text("No safety contact configured", style = MaterialTheme.typography.titleMedium,
            color = SanXTextSecondary)
        Text("Add a primary safety contact who will be alerted during emergencies.",
            style = MaterialTheme.typography.bodySmall, color = SanXTextDisabled)
    }
}

@Composable
private fun ContactDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    var smsEnabled by remember { mutableStateOf(contact?.notifyViaSms ?: true) }
    var locationEnabled by remember { mutableStateOf(contact?.shareLocation ?: true) }
    var audioEnabled by remember { mutableStateOf(contact?.shareAudio ?: false) }

    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let { contactUri ->
                try {
                    val contentResolver = context.contentResolver
                    val cursor = contentResolver.query(contactUri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            val nameValue = if (nameIndex >= 0) c.getString(nameIndex) else ""
                            
                            val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                            var contactId = if (idIndex >= 0) c.getString(idIndex) else ""
                            if (contactId.isEmpty()) {
                                contactId = contactUri.lastPathSegment ?: ""
                            }
                            
                            val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                            val hasPhone = if (hasPhoneIndex >= 0) c.getInt(hasPhoneIndex) > 0 else false
                            
                            var phoneNumberValue = ""
                            if (hasPhone) {
                                val phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )
                                phoneCursor?.use { pc ->
                                    if (pc.moveToFirst()) {
                                        val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (phoneIndex >= 0) {
                                            phoneNumberValue = pc.getString(phoneIndex)
                                        }
                                    }
                                }
                            }
                            
                            if (nameValue.isNotEmpty()) {
                                name = nameValue
                            }
                            if (phoneNumberValue.isNotEmpty()) {
                                phone = phoneNumberValue
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(SanXCard)
                .border(1.dp, SanXBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (contact == null) "Add Contact" else "Edit Contact",
                style = MaterialTheme.typography.headlineSmall,
                color = SanXTextPrimary
            )

            // Select from phonebook contacts option
            Button(
                onClick = { contactPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SanXSurface,
                    contentColor = SanXSafe
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SanXBorder)
            ) {
                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select from Contacts", style = MaterialTheme.typography.labelLarge)
            }

            SanXTextField(value = name, onValueChange = { name = it }, label = "Full Name",
                icon = Icons.Default.Person)
            SanXTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number",
                icon = Icons.Default.Phone)
            SanXTextField(value = email, onValueChange = { email = it }, label = "Email (optional)",
                icon = Icons.Default.Email)

            // Permission toggles
            PermissionToggle("SMS Notification", smsEnabled) { smsEnabled = it }
            PermissionToggle("Share Location", locationEnabled) { locationEnabled = it }
            PermissionToggle("Share Audio Evidence", audioEnabled) { audioEnabled = it }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SanXTextSecondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SanXBorder)
                    )
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSave(
                                (contact ?: Contact(name = name, phoneNumber = phone)).copy(
                                    name = name, phoneNumber = phone, email = email,
                                    priority = 1, notifyViaSms = smsEnabled,
                                    shareLocation = locationEnabled, shareAudio = audioEnabled
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SanXSafe, contentColor = Color.White)
                ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SanXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SanXSafe,
            unfocusedBorderColor = SanXBorder,
            focusedLabelColor = SanXSafe,
            unfocusedLabelColor = SanXTextSecondary,
            cursorColor = SanXSafe,
            focusedTextColor = SanXTextPrimary,
            unfocusedTextColor = SanXTextPrimary,
            focusedLeadingIconColor = SanXSafe,
            unfocusedLeadingIconColor = SanXTextDisabled
        ),
        singleLine = true
    )
}

@Composable
private fun PermissionToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SanXTextPrimary)
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SanXBlack, checkedTrackColor = SanXSafe,
                uncheckedThumbColor = SanXTextDisabled, uncheckedTrackColor = SanXCard
            )
        )
    }
}
