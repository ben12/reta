module com.ben12.reta.api
{
	requires transitive javafx.controls;

	requires transitive jakarta.validation;

	requires com.google.common;

	requires java.logging;

	requires java.desktop;

	requires org.hibernate.validator;

	requires ini4j;

	exports com.ben12.reta.api;

	exports com.ben12.reta.beans.constraints;

	exports com.ben12.reta.beans.constraints.validator;

	exports com.ben12.reta.beans.property.buffering;

	exports com.ben12.reta.beans.property.validation;

	exports com.ben12.reta.plugin;

	exports com.ben12.reta.view.control;

	exports com.ben12.reta.view.validation;
}
