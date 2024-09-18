# 3DOrganoidAnalysis_
 - This repository contains a robust methodology for identifying and characterizing organoids using the Cellpose algorithm.
- The script allows for precise 3D identification of individual organoids based on the overlay of GFP and Tomato markers
  
## Download 3DOrganoidAnalysis_
1. Go to the ``GitHub`` repository
2. Click on ``<Code>``>``Download ZIP``
3. The repo will be found at ``Downloads`` directory.
4. 
## Running 3DOrganoidAnalysis_ in headless mode through ImageJ/Windows Windows Terminal (ALL parameters)

``ImageJ-win64.exe --ij2 --headless --run "/absolute_path/to/groovyscript/3DOrganoidAnalysisAll_.groovy" "headless=true``

## Features
- 3D Identification: Utilizes the Cellpose algorithm for accurate 3D segmentation of organoids.
- Fluorescence Analysis: Calculates fluorescence intensity distributions for GFP and Tomato markers for each segmented organoid.
- Volume Analysis both in physical units and pixels.
- Fluorescence Metrics: Computes maximum, minimum, mean, and integrated density of fluorescence intensities for both GFP and Tomato markers.
- Classification:
  - Organoids are classified as GFP-positive (GFP+) or GFP-negative (GFP-), and Tomato-positive (Tomato+) or Tomato-negative (Tomato-) based on fluorescence expression within a predefined eroded area.
  - Further classification of GFP+ organoids based on GFP intensity distribution:
    - Full: GFP+ intensity above the mean GFP intensity plus two standard deviations.
    - Empty: GFP+ intensity below this threshold.
 - Similar classification procedure for Tomato+ organoids.
