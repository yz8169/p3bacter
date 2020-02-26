import numpy as np
import pandas as pd
import plotly
import plotly.graph_objs as go
import argparse


def dealHeader(df):
    tmpColumns = df.columns.values
    tmpColumns[0] = "Id"
    df.columns = tmpColumns


parser = argparse.ArgumentParser()
parser.add_argument("-m", type=str, default="hand", help="group offer way")
parser.add_argument("-x", type=str, default="1", help="x axis pca name")
parser.add_argument("-y", type=str, default="2", help="y axis pca name")
args = parser.parse_args()
df = pd.read_csv("out.txt", sep='\t')
groupDf = pd.read_csv("group.txt", sep='\t')
impoDf = pd.read_csv("impoPath.txt", sep='\t')
dealHeader(df)
dealHeader(impoDf)
x = "PC" + args.x
y = "PC" + args.y
group = go.Scatter(
    x=df[x],
    y=df[y],
    mode='markers',
    marker=dict(
        color='#FF9931',
        size=12,
    ),
    text=df["Id"].apply(lambda x: "Sample:" + x),
    hoverinfo='x+y+text',
    name="group"
)
data = [group]

if args.m != "hand":
    data = []
    groups = groupDf.groupby("Group")
    for name, group in groups:
        eachGroupDf = df[df["Id"].isin(group["Sample"])]
        eachGroup = go.Scatter(
            x=eachGroupDf[x],
            y=eachGroupDf[y],
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
    width=800,
    height=600,
    title="PCA",
    xaxis=dict(
        title=x + '(' + str(impoDf["x"][impoDf["Id"] == x].values[0] * 100) + "%)",
        showgrid=False,
        ticks='outside',
        showline=True,
    ),
    yaxis=dict(
        title=y + '(' + str(impoDf["x"][impoDf["Id"] == y].values[0] * 100) + "%)",
        showgrid=False,
        ticks='outside',
        showline=True,
    ),
    hovermode="closest",
)
config = dict(
    displaylogo=False,
    showLink=False,
    modeBarButtonsToRemove=["autosizable","scrollZoom"],
)
fig = dict(data=data, layout=layout)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()
