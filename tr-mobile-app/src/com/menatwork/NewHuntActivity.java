package com.menatwork;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.menatwork.skills.SkillButtonFactory;

public class NewHuntActivity extends GuiTalentRadarActivity {

	private ViewGroup necessarySkillsContainer;
	private ViewGroup optionalSkillsContainer;
	private ImageButton addNecessarySkillButton;
	private ImageButton addOptionalSkillButton;
	private TextView necessarySkillTextView;
	private TextView optionalSkillTextView;
	private TextView nameTextView;
	private ViewGroup optionalSkillsSuggestionsContainer;
	private ViewGroup necessarySkillsSuggestionsContainer;

	@Override
	protected void postCreate(final Bundle savedInstanceState) {
		this.initializeContainers();
		this.initializeTextWatchers();
	}

	private void initializeTextWatchers() {
		this.necessarySkillTextView
				.addTextChangedListener(new SkillsTextWatcher(
						addNecessarySkillButton));
		this.optionalSkillTextView
				.addTextChangedListener(new SkillsTextWatcher(
						addOptionalSkillButton));
	}

	private void initializeContainers() {
		necessarySkillsContainer.removeAllViews();
		optionalSkillsContainer.removeAllViews();
		necessarySkillsContainer.addView(getTalentRadarApplication()
				.getSkillButtonFactory().getEmptySkillsButton(this));
		optionalSkillsContainer.addView(getTalentRadarApplication()
				.getSkillButtonFactory().getEmptySkillsButton(this));
	}

	@Override
	protected void setupButtons() {
		addNecessarySkillButton.setOnClickListener(new AddSkillOnClickListener(
				necessarySkillsContainer, necessarySkillTextView));
		addOptionalSkillButton.setOnClickListener(new AddSkillOnClickListener(
				optionalSkillsContainer, optionalSkillTextView));
		addNecessarySkillButton.setEnabled(false);
		addOptionalSkillButton.setEnabled(false);
	}

	@Override
	protected void findViewElements() {
		necessarySkillsContainer = findViewGroupById(R.id.new_hunt_layout_necessary_skills);
		necessarySkillsSuggestionsContainer = findViewGroupById(R.id.new_hunt_layout_suggestions_necessary_skills);
		optionalSkillsContainer = findViewGroupById(R.id.new_hunt_layout_optional_skills);
		optionalSkillsSuggestionsContainer = findViewGroupById(R.id.new_hunt_layout_suggestions_optional_skills);
		addNecessarySkillButton = findImageButtonById(R.id.new_hunt_button_add_necessary_skill);
		addOptionalSkillButton = findImageButtonById(R.id.new_hunt_button_add_optional_skill);
		necessarySkillTextView = findTextViewById(R.id.new_hunt_necessary_skill_input);
		optionalSkillTextView = findTextViewById(R.id.new_hunt_optional_skill_input);
		nameTextView = findTextViewById(R.id.new_hunt_name);
	}

	@Override
	protected int getViewLayoutId() {
		return R.layout.new_hunt;
	}

	protected void removeNecessarySkill(final View buttonToDelete) {
		necessarySkillsContainer.removeView(buttonToDelete);
		if (necessarySkillsContainer.getChildCount() == 0)
			necessarySkillsContainer.addView(getTalentRadarApplication()
					.getSkillButtonFactory().getEmptySkillsButton(this));
		// remove skill from hunt model or whatever
	}

	protected void removeOptionalSkill(final View buttonToDelete) {
		optionalSkillsContainer.removeView(buttonToDelete);
		if (optionalSkillsContainer.getChildCount() == 0)
			optionalSkillsContainer.addView(getTalentRadarApplication()
					.getSkillButtonFactory().getEmptySkillsButton(this));
		// remove skill from hunt model or whatever
	}

	private class AddSkillOnClickListener implements OnClickListener {
		private final ViewGroup destinationContainer;
		private final TextView input;

		private AddSkillOnClickListener(final ViewGroup destinationContainer,
				final TextView input) {
			super();
			this.destinationContainer = destinationContainer;
			this.input = input;
		}

		@Override
		public void onClick(final View arg0) {
			final SkillButtonFactory skillButtonFactory = getTalentRadarApplication()
					.getSkillButtonFactory();
			final String newSkillText = input.getText().toString();
			final Button newSkillButton = skillButtonFactory.getSkillButton(
					NewHuntActivity.this, newSkillText);
			newSkillButton
					.setOnLongClickListener(new SkillOnLongClickListener());

			// remove empty skill button if present
			if (destinationContainer.getChildCount() == 1)
				if (destinationContainer.getChildAt(0).equals(
						skillButtonFactory
								.getEmptySkillsButton(NewHuntActivity.this)))
					destinationContainer.removeAllViews();

			destinationContainer.addView(newSkillButton);
			input.setText("");
		}

	}

	private class SkillOnLongClickListener implements OnLongClickListener {

		@Override
		public boolean onLongClick(final View v) {
			if (v != null) {
				final Builder builder = new AlertDialog.Builder(
						NewHuntActivity.this);
				builder.setItems(new CharSequence[] { "Eliminar skill" },
						new DeleteSkillListener(v));
				builder.create().show();
				return true;
			}
			return true;
		}

		private class DeleteSkillListener implements
				DialogInterface.OnClickListener {

			private final View buttonToDelete;

			public DeleteSkillListener(final View v) {
				this.buttonToDelete = v;
			}

			@Override
			public void onClick(final DialogInterface arg0, final int arg1) {
				if (findViewInViewGroup(necessarySkillsContainer,
						buttonToDelete))
					removeNecessarySkill(buttonToDelete);
				else if (findViewInViewGroup(optionalSkillsContainer,
						buttonToDelete))
					removeOptionalSkill(buttonToDelete);
			}

			private boolean findViewInViewGroup(final ViewGroup viewGroup,
					final View view) {
				boolean found = false;
				final int childCount = viewGroup.getChildCount();
				for (int i = 0; i < childCount; i++)
					if (view.equals(viewGroup.getChildAt(i)))
						found = true;
				return found;
			}
		}
	}

	private class SkillsTextWatcher implements TextWatcher {

		private final ImageButton button;

		public SkillsTextWatcher(final ImageButton button) {
			this.button = button;
		}

		@Override
		public void afterTextChanged(final Editable editable) {
			if (editable == null)
				return;
			final String inputString = editable.toString();
			button.setEnabled(inputString.length() > 0);
			// TODO - suggestions sample snippet
			// if ("java".startsWith(inputString.toLowerCase()))
			// necessarySkillsSuggestionsContainer
			// .addView(getTalentRadarApplication()
			// .getSkillButtonFactory().getSkillButton(
			// NewHuntActivity.this, "Java"));
		}

		@Override
		public void beforeTextChanged(final CharSequence arg0, final int arg1,
				final int arg2, final int arg3) {
		}

		@Override
		public void onTextChanged(final CharSequence arg0, final int arg1,
				final int arg2, final int arg3) {
		}

	}

}
