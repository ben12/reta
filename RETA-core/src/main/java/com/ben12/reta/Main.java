// Package : com.ben12.reta
// File : Main.java
// 
// Copyright (C) 2014 Benoît Moreau (ben.12)
//
// This file is part of RETA (Requirement Engineering Traceability Analysis).
//
// RETA is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// RETA is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with RETA.  If not, see <http://www.gnu.org/licenses/>.

package com.ben12.reta;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.ben12.reta.view.MainConfigurationController;
import com.ben12.reta.view.control.MessageDialog;

/**
 * @author Benoît Moreau (ben.12)
 */
public class Main extends Application
{
	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(final Stage stage) throws Exception
	{
		try
		{
			MessageDialog.setDefaultParent(stage);

			final ResourceBundle labels = ResourceBundle.getBundle("com/ben12/reta/view/Labels");

			final FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("view/MainConfigurationUI.fxml"));
			loader.setResources(labels);
			final Parent root = (Parent) loader.load();

			stage.setScene(new Scene(root));
			stage.setTitle(labels.getString("title"));
			stage.sizeToScene();
			stage.show();

			final Parameters parameters = getParameters();
			final List<String> args = parameters.getRaw();
			if (args.size() > 0)
			{
				final File retaFile = new File(args.get(0));
				((MainConfigurationController) loader.getController()).open(retaFile);
			}
		}
		catch (final Exception e)
		{
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", e);
			System.exit(0);
		}
	}

	/**
	 * @param args
	 *            application arguments
	 */
	public static void main(final String[] args)
	{
		try
		{
			LogManager.getLogManager().readConfiguration(
					Main.class.getResourceAsStream("/com/ben12/reta/resources/logging/logging.properties"));
		}
		catch (final IOException e)
		{
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "", e);
		}

		Application.launch(Main.class, args);
	}
}
