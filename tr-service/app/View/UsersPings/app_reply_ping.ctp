<?php 
echo $this->Form->create('UsersPing');
	echo $this->Form->input('id', array('type' => 'select'));
	echo $this->Form->input('user_to_id', array('options' => $users));
	echo $this->Form->input('response', array('type' => 'select'));
	echo $this->Form->submit();
echo $this->Form->end();
?>