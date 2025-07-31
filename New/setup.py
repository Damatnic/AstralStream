from setuptools import setup, find_packages

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setup(
    name="astralstream-elite-agent",
    version="1.0.0",
    author="AstralStream Team",
    author_email="noreply@astralstream.dev",
    description="Elite upgrade agent for AstralStream video player - Transform your Android video player into a 10/10 enterprise application",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/astralstream/elite-agent",
    packages=find_packages(),
    classifiers=[
        "Development Status :: 5 - Production/Stable",
        "Intended Audience :: Developers",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Topic :: Software Development :: Code Generators",
        "Topic :: Software Development :: Quality Assurance",
        "Topic :: Multimedia :: Video :: Display",
    ],
    install_requires=[
        "pyyaml>=6.0",
        "pathlib2>=2.3.7",
        "click>=8.1.0",
        "colorama>=0.4.6",
        "tqdm>=4.65.0",
        "jinja2>=3.1.2",
        "typing-extensions>=4.5.0",
    ],
    entry_points={
        "console_scripts": [
            "astral-upgrade=astralstream_elite_agent:main",
            "astralstream-elite=astralstream_elite_agent:main",
        ],
    },
    python_requires=">=3.8",
    project_urls={
        "Bug Reports": "https://github.com/astralstream/elite-agent/issues",
        "Source": "https://github.com/astralstream/elite-agent",
        "Documentation": "https://docs.astralstream.dev/elite-agent",
    },
)