# Advancing-Wi-Fi-Localization-for-Enhanced-Campus-and-Indoor-Experiences
Graduation project from Zewail City focusing on advancing Wi-Fi localization for enhanced indoor experiences. Combines ray tracing techniques with machine learning to improve accuracy and reliability in indoor positioning. Utilizes existing Wi-Fi infrastructure for scalable and cost-effective solutions.
# Advancing Wi-Fi Localization for Enhanced Campus and Indoor Experiences

## Project Overview
This project addresses the challenge of accurate indoor Wi-Fi localization, a crucial need in various sectors like healthcare, retail, and logistics. The motivation stems from the limitations of GPS in indoor environments and the growing demand for precise indoor navigation systems. Our proposed solution integrates advanced ray tracing techniques with machine learning algorithms to interpret complex signal interactions for high-precision positioning. This approach leverages existing Wi-Fi infrastructure, making it economically viable and scalable.

## Table of Contents
- [Introduction](#introduction)
- [Motivation](#motivation)
- [Challenges and Scope](#challenges-and-scope)
- [Proposed Methodology](#proposed-methodology)
  - [Enhanced CIR Model with Theoretical Simulations](#enhanced-cir-model-with-theoretical-simulations)
  - [Practical App Development with RSSI Data](#practical-app-development-with-rssi-data)
  - [Integration of Data Sources](#integration-of-data-sources)
- [Implementation](#implementation)
- [Testing and Evaluation](#testing-and-evaluation)
- [Results](#results)
- [Conclusion and Future Work](#conclusion-and-future-work)
- [Contributors](#contributors)
- [Acknowledgements](#acknowledgements)

## Introduction
The increasing reliance on interconnected devices within indoor environments necessitates the development of accurate and reliable indoor device localization systems. This project investigates the refinement and amplification of Wi-Fi technology’s potential for precise indoor positioning.

## Motivation
Precise indoor positioning unlocks significant economic benefits. In retail environments, personalized promotions based on customer location can be implemented. Healthcare facilities can optimize patient tracking and asset management. Logistics companies can streamline operations, all relying on accurate indoor positioning data.

## Challenges and Scope
Developing a high-fidelity Wi-Fi localization system necessitates navigating a complex archipelago of challenges, including technical intricacies, algorithmic efficiency, and data privacy and security.

## Proposed Methodology
### Enhanced CIR Model with Theoretical Simulations
We gather detailed environmental data by obtaining actual maps or CAD files of the building. This allows us to perform ray tracing, which generates Channel Impulse Response (CIR) data. The CIR data provides critical insights into the environment, such as obstacles and signal propagation paths, essential for precise localization.

### Practical App Development with RSSI Data
In scenarios where actual maps or CAD files are not available, we rely on data collected from users. This includes Received Signal Strength Indicator (RSSI) and magnetometer readings from their smartphones. The mobile application we developed scans for available Wi-Fi access points, records the RSSI values, and collects magnetometer data.

### Integration of Data Sources
When both actual maps/CAD files and user-provided data are available, we integrate the findings from both sources. By combining the detailed environmental characteristics obtained through ray tracing with the dynamic data collected from users, we achieve higher accuracy in indoor localization.

## Implementation
The implementation section covers the development process, challenges encountered, and solutions implemented. This includes the development of robust prototypes, followed by rigorous testing under controlled and real-world conditions.

## Testing and Evaluation
The system underwent rigorous unit, integration, and performance testing to evaluate its accuracy and reliability. The results were promising, showcasing high precision in localization and effective handling of common issues like signal interference.

## Results
The project's results indicated high precision in localization and effective handling of common issues like signal interference. The system’s performance in varied environments indicated its potential for widespread practical application.

## Conclusion and Future Work
The project demonstrates the feasibility of using Wi-Fi for accurate indoor localization. It opens avenues for further research and development in this field, with implications for enhanced safety, improved user experience, and operational efficiency in indoor spaces.

## Contributors
- **Eman Allam** - [LinkedIn](https://www.linkedin.com/in/emanallam)
- **Samaa Khair** - [LinkedIn](https://www.linkedin.com/in/samaakhair)

## Acknowledgements
We would like to express our sincere gratitude to our supervisors Dr. Samy S. Soliman and Dr. Mahmoud Abdelaziz for their invaluable guidance and support throughout this project. Special thanks to our collaborator Mohamed Khaled for his continuous assistance. We also extend our heartfelt gratitude to our families and friends for their unwavering support.
[Team10_CIE599_Final_Report_Draft__Copy_.pdf](https://github.com/user-attachments/files/15755905/Team10_CIE599_Final_Report_Draft__Copy_.pdf)

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
