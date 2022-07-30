# Android Sensor Multiplexing PoCs

Accompanying PoCs for the paper titled ["Exploiting Sensor Multiplexing for Covert Channels and Application Fingerprinting on Mobile Devices"](https://arxiv.org/abs/2110.06363) (arXiv:2110.06363).

Tested on a Google Pixel 4A (Qualcomm SDM730 SnapDragon 730G; Android 11, build RQ2A.210405.005), but can be modified to support other devices (e.g. Xiaomi Pocofone) using the sampling constants in the paper's appendices.

Untested devices may also be vulnerable, but appropriate sampling periods/frequencies will need defined.

## Android Studio Projects

- [covert-channels](https://github.com/cgshep/android-multiplexing-security-pocs/covert-channels) 
- [application-fingerprinting](https://github.com/cgshep/android-multiplexing-security-pocs/application-fingerprinting)

## Citation

```bibtex
@misc{shepherd2021exploiting,
      title={A side-channel analysis of sensor multiplexing for covert channels and application fingerprinting on mobile devices},
      author={Carlton Shepherd and Jan Kalbantner and Benjamin Semal and Konstantinos Markantonakis},
      year={2022},
      eprint={2110.06363},
      archivePrefix={arXiv},
      primaryClass={cs.CR}
}
```
