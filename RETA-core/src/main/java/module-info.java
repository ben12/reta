module com.ben12.reta.core
{
	requires javafx.controls;

	requires transitive jakarta.validation;

	requires com.google.common;

	requires transitive java.logging;

	requires javafx.fxml;

	requires transitive com.ben12.reta.api;

	requires org.hibernate.validator;

	requires java.desktop;

	requires org.apache.poi.ooxml;

	requires ini4j;

	exports com.ben12.reta to javafx.graphics;

	exports com.ben12.reta.util.logging to java.logging;

	exports com.ben12.reta.view to javafx.fxml;

	exports com.ben12.reta.constraints to org.hibernate.validator;

	exports com.ben12.reta.constraints.validator to org.hibernate.validator;

	exports com.ben12.reta.model to com.ben12.reta.api;

	opens com.ben12.reta.util to org.hibernate.validator;

	opens com.ben12.reta.view to javafx.fxml;

	opens com.ben12.reta.model to org.hibernate.validator;

	uses com.ben12.reta.plugin.SourceProviderPlugin;
}
