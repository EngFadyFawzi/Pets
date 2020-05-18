package com.engineerfadyfawzi.pets;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.engineerfadyfawzi.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks< Cursor >
{
    /**
     * Identifies for the pet data loader to edit
     */
    private static final int EXISTING_PET_LOADER = 1;
    
    /**
     * Content URI for the existing pet (null if it's a new pet)
     */
    private Uri mEditPetUri;
    
    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;
    
    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;
    
    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;
    
    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;
    
    /**
     * Boolean flag that keeps of whether the pet has been edited (true) or not (false).
     */
    private boolean mPetHasChanged = false;
    
    /**
     * OnTouchListener that listens for any user touches on a View,
     * implying that they are modifying the view, and we change the mPetHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch( View view, MotionEvent motionEvent )
        {
            mPetHasChanged = true;
            return false;
        }
    };
    
    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_editor );
        
        if ( savedInstanceState != null )
            mPetHasChanged = savedInstanceState.getBoolean( "mPetHasChanged" );
        
        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new pet or editing an existing one.
        // Use getIntent() and getData() to get the associated URI.
        mEditPetUri = getIntent().getData();
        
        // Set title of EditorActivity on which situation we have.
        // If the EditorActivity was opened using the ListView item,
        // then we will have uri of pet so change app bar to say "Edit Pet"
        // Otherwise if this is a new pet, uri is null so change app bar to say "Add a pet"
        
        // If the intent DOES NOT contain a pet content UrI, then we know that we are creating a new pet.
        if ( mEditPetUri == null )
        {
            // This is a new pet, so change the app bar to say "Add a Pet"
            setTitle( R.string.editor_activity_title_new_pet );
            
            // Invalidate teh options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        }
        else
        {
            // Otherwise this is an existing pet, so change app bar to say "Edit Pet"
            setTitle( getString( R.string.editor_activity_title_edit_pet ) );
            
            // Initializes a loader to read the pet data from the database,
            // and display the current values in the editor.
            getSupportLoaderManager().initLoader( EXISTING_PET_LOADER, null, this );
        }
        
        // Find all relevant views that we will need to read user input from.
        mNameEditText = findViewById( R.id.edit_pet_name );
        mBreedEditText = findViewById( R.id.edit_pet_breed );
        mWeightEditText = findViewById( R.id.edit_pet_weight );
        mGenderSpinner = findViewById( R.id.spinner_gender );
        
        // Setup onTouchListener on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener( mTouchListener );
        mBreedEditText.setOnTouchListener( mTouchListener );
        mWeightEditText.setOnTouchListener( mTouchListener );
        mGenderSpinner.setOnTouchListener( mTouchListener );
        
        setupSpinner();
    }
    
    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner()
    {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource( this,
                R.array.array_gender_option, android.R.layout.simple_spinner_item );
        
        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_dropdown_item_1line );
        
        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter( genderSpinnerAdapter );
        
        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected( AdapterView< ? > parent, View view, int position, long id )
            {
                String selection = ( String ) parent.getItemAtPosition( position );
                if ( !TextUtils.isEmpty( selection ) )
                {
                    if ( selection.equals( getString( R.string.gender_male ) ) )
                        mGender = PetEntry.GENDER_MALE;
                    else if ( selection.equals( getString( R.string.gender_female ) ) )
                        mGender = PetEntry.GENDER_FEMALE;
                    else
                        mGender = PetEntry.GENDER_UNKNOWN;
                }
            }
            
            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected( AdapterView< ? > adapterView )
            {
                mGender = PetEntry.GENDER_UNKNOWN;
            }
        } );
    }
    
    /**
     * Get user input from editor and save new pet into database.
     */
    private void savePet()
    {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String petName = mNameEditText.getText().toString().trim();
        String petBreed = mBreedEditText.getText().toString();
        int petGender = mGender;
        int petWeight = 0;
        
        // Check if this is supposed to be a new pet,
        // and check if all the fields in the editor are blank.
        if ( TextUtils.isEmpty( petName ) )
        {
            // Since no name for a pet was modified, we can return early without creating a new pet.
            // No need to create ContentValues and no need to do any ContentProvider operations,
            // when the pet name must be NOT NULL.
            Toast.makeText( this, getString( R.string.editor_unsaved_name_required ),
                    Toast.LENGTH_LONG ).show();
            return;
            
            // If a name wasn't input that would be the only scenario that would give error.
            // No need to worry about Breed it's allow to be null or even empty,
            // No need to worry about Gender, as it will say Unknown by default,
            // No need to worry about Weight it will set to a default value = 0 if it wasn't modified.
        }
        
        try
        {
            // If the user doesn't enter a weight the text field will be empty string "" (No user input).
            // This throws NumberFormatException because we are trying to convert an empty string to an integer
            petWeight = Integer.parseInt( mWeightEditText.getText().toString().trim() );
        }
        catch ( NumberFormatException numberFormatException )
        {
            petWeight = 0; // I set the weight to 0 because this is the default value in the database so..
        }
        
        // Create a ContentValues object where column names are the keys,
        // and a new pet attributes are the values.
        ContentValues values = new ContentValues();
        values.put( PetEntry.COLUMN_PET_NAME, petName );
        values.put( PetEntry.COLUMN_PET_BREED, petBreed );
        values.put( PetEntry.COLUMN_PET_GENDER, petGender );
        values.put( PetEntry.COLUMN_PET_WEIGHT, petWeight );
        
        // Determine if this is a new or existing pet by checking if mEditPetUri is null or not
        if ( mEditPetUri == null )
            // This is a NEW pet, so insert a new pet into the provider,
            //  returning the content URI for the new pet.
            insertPet( values );
        else
            // Otherwise this is an Existing pet, so update the pet with content URI: mEditPetUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args,
            // because mEditPetUri will already identify the correct row in the database that
            // we want to modify.
            updatePet( values );
    }
    
    /**
     * Helper method to insert a new pet into database using provider.
     *
     * @param values
     */
    private void insertPet( ContentValues values )
    {
        // This is a NEW pet, so insert a new pet into the provider,
        //  returning the content URI for the new pet.
        Uri newUri = getContentResolver().insert( PetEntry.CONTENT_URI, values );
        
        // Show a toast message depending on whether or not the insertion was successful
        if ( newUri == null )
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText( this, getString( R.string.editor_insert_pet_failed ),
                    Toast.LENGTH_SHORT ).show();
        else
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText( this, getString( R.string.editor_insert_pet_successful ),
                    Toast.LENGTH_SHORT ).show();
    }
    
    /**
     * Helper method to update an existing pet in database using provider.
     *
     * @param values
     */
    private void updatePet( ContentValues values )
    {
        // Update an existing pet into the provider, retuning the integer represents rows updated
        int rowsUpdated = getContentResolver().update( mEditPetUri, values, null, null );
        
        // Show a toast message depending on whether or not the update was successful.
        if ( rowsUpdated == 0 )
            // If no rows were affected, then there was an error with the update.
            Toast.makeText( this, getString( R.string.editor_update_pet_failed ),
                    Toast.LENGTH_SHORT ).show();
        else
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText( this, getString( R.string.editor_update_pet_successful ),
                    Toast.LENGTH_SHORT ).show();
    }
    
    /**
     * Helper method to preform the deletion of the pet in the database
     */
    private void deletePet()
    {
        // COMPLETED: Implement this method
        
        // Only perform the delete if this is an existing pet.
        if ( mEditPetUri != null )
        {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mEditPetUri
            // content URI already identifies the pet that we want (to delete).
            // Delete an existing pet into the provider, retuning the integer represents rows deleted
            int rowsDeleted = getContentResolver().delete( mEditPetUri, null, null );
            
            // Show a toast message depending on whether or not the delete was successful.
            if ( rowsDeleted == 0 )
                // If no rows were affected, then there was an error with the delete.
                Toast.makeText( this, getString( R.string.editor_delete_pet_failed ),
                        Toast.LENGTH_SHORT ).show();
            else
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText( this, getString( R.string.editor_delete_pet_successful ),
                        Toast.LENGTH_SHORT ).show();
            
            // Optionally you could add it after the successful delete toast, would make more sense,
            // because if the pet couldn't have been deleted, you would still be in the EditorActivity,
            // but that depends on your preference.
            
            // Close the activity
            finish();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate( R.menu.menu_editor, menu );
        return true;
    }
    
    /**
     * This method is called after invalidateOptionMenu(), so that the menu can be updated.
     * (some menu items can be hidden or made visible).
     *
     * @param menu
     *
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        super.onPrepareOptionsMenu( menu );
        
        // If this is a new pet, hide the "Delete" menu item.
        if ( mEditPetUri == null )
        {
            MenuItem menuItem = menu.findItem( R.id.action_delete );
            menuItem.setVisible( false );
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        // User clicked on a menu option in the app bar overflow menu
        switch ( item.getItemId() )
        {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                savePet();
                // Exit activity (return to previous one)
                finish();
                return true;
            
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if ( !mPetHasChanged )
                {
                    // Navigate back to parent activity (CatalogActivity)
                    NavUtils.navigateUpFromSameTask( this );
                    return true;
                }
                
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick( DialogInterface dialogInterface, int id )
                            {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask( EditorActivity.this );
                            }
                        };
                
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangedDialog( discardButtonClickListener );
                return true;
        }
        
        return super.onOptionsItemSelected( item );
    }
    
    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed()
    {
        // If the pet hasn't changed, continue with handling back button press
        if ( !mPetHasChanged )
        {
            super.onBackPressed();
            return;
        }
        
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick( DialogInterface dialogInterface, int id )
                    {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        
        // Show dialog that there are unsaved changes
        showUnsavedChangedDialog( discardButtonClickListener );
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState( outState );
        outState.putBoolean( "mPetHasChanged", mPetHasChanged );
    }
    
    @Override
    public Loader< Cursor > onCreateLoader( int id, Bundle args )
    {
        // Since the editor shows all pet attributes, define a projection that contains
        // all columns from the pet table.
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT };
        
        // This loader will execute the ContentProvider's query method on a background thread.
        return new CursorLoader(
                this,               // Parent activity context
                mEditPetUri,                // Query the content URI for the current pet
                projection,                 // Columns to include in the resulting Cursor
                null,               // No selection clause
                null,           // No selection arguments
                null );             // Default sort order
    }
    
    @Override
    public void onLoadFinished( Loader< Cursor > loader, Cursor cursor )
    {
        // Bail early if the cursor is null or there is less than 1 row in the cursor.
        if ( cursor == null || cursor.getCount() < 1 )
            return;
        
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if ( cursor.moveToFirst() )
        {
            // Find the columns of pet attributes that we're interested in.
            int petNameColumnIndex = cursor.getColumnIndex( PetEntry.COLUMN_PET_NAME );
            int petBreedColumnIndex = cursor.getColumnIndex( PetEntry.COLUMN_PET_BREED );
            int petGenderColumnIndex = cursor.getColumnIndex( PetEntry.COLUMN_PET_GENDER );
            int petWeightColumnIndex = cursor.getColumnIndex( PetEntry.COLUMN_PET_WEIGHT );
            
            // Extract out the values from the Cursor for the given column index.
            String petName = cursor.getString( petNameColumnIndex );
            String petBreed = cursor.getString( petBreedColumnIndex );
            int petGender = cursor.getInt( petGenderColumnIndex );
            int petWeight = cursor.getInt( petWeightColumnIndex );
            
            // Update the views on the screen with the values form the database
            updateInputs( petName, petBreed, petGender, petWeight );
        }
    }
    
    /**
     * Update the view on the screen with the values from the database.
     *
     * @param petName
     * @param petBreed
     * @param petGender
     * @param petWeight
     */
    private void updateInputs( String petName, String petBreed, int petGender, int petWeight )
    {
        mNameEditText.setText( petName );
        mBreedEditText.setText( petBreed );
        // Call setSelection() so that option is displayed on screen as the current selection
        mGenderSpinner.setSelection( petGender );
        mWeightEditText.setText( String.valueOf( petWeight ) );
    }
    
    @Override
    public void onLoaderReset( Loader< Cursor > loader )
    {
        // If the loader is invalidated, clear all teh data from the input fields.
        clearInputs();
    }
    
    /**
     * If the loader is invalidated, clear out all the data from the input fields.
     */
    private void clearInputs()
    {
        mNameEditText.setText( "" );
        mBreedEditText.setText( "" );
        mWeightEditText.setText( "" );
        mGenderSpinner.setSelection( 0 ); // Select "Unknown" gender
    }
    
    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when the user confirms
     *                                   they want to discard their changes (continue LEAVING).
     */
    private void showUnsavedChangedDialog( DialogInterface.OnClickListener discardButtonClickListener )
    {
        // Create an AlertDialog.Builder and set the message, and click listeners
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.unsaved_changes_dialog_msg );
        // For the positive and negative buttons on the dialog.
        builder.setPositiveButton( R.string.discard, discardButtonClickListener );
        builder.setNegativeButton( R.string.keep_editing, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int id )
            {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                // Reason of this if statement: Sometimes unexpected things could be happen like
                // alert popup, user click outside the modal (prematurely closing it), api error, etc.
                // It's a good measure to check just to be sure that there is something to close
                // before you actually close it.
                if ( dialogInterface != null )
                    dialogInterface.dismiss();
            }
        } );
        
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    
    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteConfirmationDialog()
    {
        // Create an AlertDialog.Builder and set the message, and click listeners
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.delete_dialog_msg );
        
        // For the positive and negative buttons on the dialog.
        builder.setPositiveButton( R.string.delete, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int id )
            {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        } );
        
        builder.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialogInterface, int id )
            {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if ( dialogInterface != null )
                    dialogInterface.dismiss();
            }
        } );
        
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}