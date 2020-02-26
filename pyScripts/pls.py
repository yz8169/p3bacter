import numpy as np
import pandas as pd
import plotly
import plotly.graph_objs as go
import argparse


def dealHeader(df):
    tmpColumns = df.columns.values
    tmpColumns[0] = "Id"
    df.columns = tmpColumns


df = pd.read_csv("site.txt", sep='\t')
groupDf = pd.read_csv("group.txt", sep='\t')
impoDf = pd.read_csv("impo.txt", sep='\t')
dealHeader(df)
dealHeader(impoDf)
data = []
groups = groupDf.groupby("Group")
for name, group in groups:
    eachGroupDf = df[df["Id"].isin(group["Sample"])]
    eachGroup = go.Scatter(
        x=eachGroupDf["comp 1"],
        y=eachGroupDf["comp 2"],
        mode='markers',
        marker=dict(
            size=12,
        ),
        text=eachGroupDf["Id"].apply(lambda x: "Sample:" + x),
        hoverinfo='x+y+text',
        name=name
    )
    data.append(eachGroup)
layout = go.Layout(
    title="Plsda",
    xaxis=dict(
        title='pls1:' + str(impoDf.ix[0]["comp 1"] * 100) + "%",
        showgrid=False,
        ticks='outside',
        zeroline=False,
        showline=True,
    ),
    yaxis=dict(
        title='pls2:' + str(impoDf.ix[0]["comp 2"] * 100) + "%",
        showgrid=False,
        zeroline=False,
        ticks='outside',
        showline=True,
    ),
    hovermode="closest",
)
config = dict(
    displaylogo=False,
    showLink=False,
)
fig = dict(data=data, layout=layout)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()
