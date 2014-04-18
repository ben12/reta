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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.ben12.reta.util.RETAAnalysis;

/**
 * @author Benoît Moreau (ben.12)
 */
public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Path doc = Paths.get("data", "analysis.ini");

		JOptionPane optionPane = new JOptionPane("Successful analyse.", JOptionPane.INFORMATION_MESSAGE);
		optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent p)
			{
				System.exit(0);
			}
		});
		JFrame frame = new JFrame();
		frame.getContentPane().add(optionPane);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try
		{
			RETAAnalysis.getInstance().configure(doc.toFile());
			RETAAnalysis.getInstance().parse();
			RETAAnalysis.getInstance().analyse();

			try
			{
				// System.out.println(RETAAnalysis.getInstance().toString());

				RETAAnalysis.getInstance().writeExcel();

				optionPane.setMessage("Successful analyse.");
			}
			catch (Exception e)
			{
				e.printStackTrace();

				optionPane.setMessage("Error during write excel.");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();

			optionPane.setMessage("Error during analyse.");
		}

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
