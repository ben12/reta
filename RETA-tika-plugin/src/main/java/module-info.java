module com.ben12.reta.tika
{
	requires java.logging;

	requires javafx.fxml;

	requires javafx.graphics;

	requires com.ben12.reta.api;

	requires ini4j;

	requires com.google.common;

	requires javafx.controls;

	requires org.hibernate.validator;

	requires transitive jakarta.validation;

	requires org.apache.tika.core;

	requires java.xml;

	exports com.ben12.reta.plugin.tika.view to javafx.fxml;

	exports com.ben12.reta.plugin.tika.view.control to javafx.fxml;

	exports com.ben12.reta.plugin.tika.beans.constraints to org.hibernate.validator;

	exports com.ben12.reta.plugin.tika.beans.constraints.validator to org.hibernate.validator;

	opens com.ben12.reta.plugin.tika.view to javafx.fxml;

	opens com.ben12.reta.plugin.tika.model to org.hibernate.validator;

	provides com.ben12.reta.plugin.SourceProviderPlugin with com.ben12.reta.plugin.tika.TikaSourceProviderPlugin,
			com.ben12.reta.plugin.tika.TikaDirectorySourceProviderPlugin;
}
